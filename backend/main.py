from __future__ import annotations

import base64
import io
import json
import os
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Optional

import requests
from fastapi import Body, FastAPI, File, Form, UploadFile
from fastapi.responses import FileResponse, JSONResponse
from PIL import Image

from backend.db import engine, now_ms, session_scope
from backend.models import Base, ClosetItem

app = FastAPI(title="DressCode Backend", version="0.1.0")


def _env(name: str, default: str) -> str:
    v = os.getenv(name, default)
    return v.strip() if v else default


TRYON_PROVIDER = _env("TRYON_PROVIDER", "mock")  # mock | dashscope
PUBLIC_BASE_URL = _env("PUBLIC_BASE_URL", "http://127.0.0.1:8000/")
DASHSCOPE_API_KEY = _env("DASHSCOPE_API_KEY", "")
TRYON_MAX_WAIT_SECONDS = int(_env("TRYON_MAX_WAIT_SECONDS", "120") or "120")
VL_MODEL = _env("VL_MODEL", "qwen-vl-plus")
DASHSCOPE_CONNECT_TIMEOUT_SECONDS = float(_env("DASHSCOPE_CONNECT_TIMEOUT_SECONDS", "10") or "10")
DASHSCOPE_READ_TIMEOUT_SECONDS = float(_env("DASHSCOPE_READ_TIMEOUT_SECONDS", "180") or "180")

DATA_DIR = Path(os.getenv("DATA_DIR", "backend_data")).resolve()
UPLOAD_DIR = DATA_DIR / "uploads"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)


@app.on_event("startup")
def _startup():
    # 部署到云端时可改为迁移工具（alembic）；课程项目先用 create_all 足够。
    Base.metadata.create_all(bind=engine)


@dataclass
class TryOnResult:
    ok: bool
    result_image_base64: str = ""
    content_type: str = "image/jpeg"
    error: str = ""
    elapsed_ms: int = 0


@app.get("/health")
def health():
    return {"ok": True, "provider": TRYON_PROVIDER, "publicBaseUrl": PUBLIC_BASE_URL}


@app.get("/files/{name}")
def files(name: str):
    path = (UPLOAD_DIR / name).resolve()
    if not str(path).startswith(str(UPLOAD_DIR.resolve())) or not path.exists():
        return JSONResponse(status_code=404, content={"ok": False, "error": "not found"})
    # 简化：按扩展名返回
    media = "image/jpeg"
    n = name.lower()
    if n.endswith(".png"):
        media = "image/png"
    return FileResponse(str(path), media_type=media)


def _public_file_url(name: str) -> str:
    return PUBLIC_BASE_URL.rstrip("/") + f"/files/{name}"


def _guess_suffix(content_type: str) -> str:
    ct = (content_type or "").lower()
    if "png" in ct:
        return ".png"
    if "webp" in ct:
        return ".webp"
    return ".jpg"


def _parse_llm_json(text: str) -> Dict[str, Any]:
    txt = (text or "").strip()
    if "```" in txt:
        txt = txt.replace("```json", "").replace("```", "").strip()
    return json.loads(txt)


def _vl_tag_by_image_input(img_url_or_data: str) -> Dict[str, Any]:
    prompt = (
        "请根据图片内容生成穿搭/服饰标签，严格只输出 JSON，不要输出多余文本。"
        "JSON 字段："
        "category(上衣/下装/外套/连衣裙/鞋子/配饰), "
        "gender(MALE/FEMALE/UNISEX), style, season, scene, weather, colors(array), keywords(array), confidence(0-1)。"
    )
    payload = {
        "model": VL_MODEL,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": img_url_or_data}},
                    {"type": "text", "text": prompt},
                ],
            }
        ],
        "temperature": 0.2,
    }
    data = _dashscope_chat_completions(payload)
    content = ((((data.get("choices") or [{}])[0].get("message") or {}).get("content")) or "").strip()
    return _parse_llm_json(content)


@app.post("/api/closet/items")
async def closet_create_item(
    image: UploadFile = File(...),
    owner: str = Form(...),
    name: str = Form(...),
    category: str = Form(...),
    color: str = Form(""),
    season: str = Form(""),
    style: str = Form(""),
    scene: str = Form(""),
    isFavorite: bool = Form(False),
    autoTag: bool = Form(True),
):
    """
    上传衣物图片并写入数据库（用于“衣橱”模块）。
    - 图片会落盘到后端的 uploads/ 并通过 /files/{name} 对外提供访问；
    - 元数据 +（可选）AI 标签会写入 MySQL/SQLite（由 DATABASE_URL 决定）。
    """
    start = time.time()
    try:
        img_bytes = _read_upload(image)
        if not img_bytes:
            return JSONResponse(status_code=400, content={"ok": False, "error": "缺少图片内容"})

        image_name = _save_upload_bytes(img_bytes, _guess_suffix(image.content_type or ""))
        image_url = _public_file_url(image_name)

        now = now_ms()
        with session_scope() as session:
            item = ClosetItem(
                owner=(owner or "").strip(),
                name=(name or "").strip(),
                category=(category or "").strip(),
                color=(color or "").strip(),
                season=(season or "").strip(),
                style=(style or "").strip(),
                scene=(scene or "").strip(),
                is_favorite=bool(isFavorite),
                image_name=image_name,
                image_url=image_url,
                created_at=now,
                updated_at=now,
            )
            session.add(item)
            session.flush()  # 取到自增 id

            tag_result = None
            if autoTag:
                # 优先使用公网 URL（对模型拉取更友好）；如果调用超时/失败则 fallback 为 data URL（并压缩图片）。
                try:
                    if _is_public_base_url(PUBLIC_BASE_URL):
                        tag_result = _vl_tag_by_image_input(image_url)
                    else:
                        raise RuntimeError("PUBLIC_BASE_URL 非公网，改用 data URL")
                except Exception:
                    compressed = _shrink_for_vl(img_bytes)
                    tag_result = _vl_tag_by_image_input(_data_url(compressed, "image/jpeg"))
                item.tags_json = json.dumps(tag_result, ensure_ascii=False, separators=(",", ":"))
                item.tag_model = VL_MODEL
                item.tag_updated_at = now
                item.updated_at = now

            session.flush()
            out = {
                "id": item.id,
                "owner": item.owner,
                "name": item.name,
                "category": item.category,
                "color": item.color,
                "season": item.season,
                "style": item.style,
                "scene": item.scene,
                "isFavorite": item.is_favorite,
                "imageUrl": image_url,
                "tags": tag_result,
                "tagModel": item.tag_model,
                "tagUpdatedAt": item.tag_updated_at,
                "createdAt": item.created_at,
            }

        elapsed = int((time.time() - start) * 1000)
        return {"ok": True, "item": out, "elapsed_ms": elapsed}
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e), "elapsed_ms": elapsed})


@app.get("/api/closet/items")
def closet_list_items(owner: str):
    try:
        with session_scope() as session:
            rows = (
                session.query(ClosetItem)
                .filter(ClosetItem.owner == owner.strip())
                .order_by(ClosetItem.created_at.desc())
                .all()
            )
            items = []
            for it in rows:
                tags = None
                if it.tags_json:
                    try:
                        tags = json.loads(it.tags_json)
                    except Exception:
                        tags = None
                items.append(
                    {
                        "id": it.id,
                        "owner": it.owner,
                        "name": it.name,
                        "category": it.category,
                        "color": it.color,
                        "season": it.season,
                        "style": it.style,
                        "scene": it.scene,
                        "isFavorite": it.is_favorite,
                        "imageUrl": _public_file_url(it.image_name),
                        "tags": tags,
                        "tagModel": it.tag_model,
                        "tagUpdatedAt": it.tag_updated_at,
                        "createdAt": it.created_at,
                        "updatedAt": it.updated_at,
                    }
                )
            return {"ok": True, "items": items}
    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


@app.patch("/api/closet/items/{item_id}")
def closet_patch_item(item_id: int, payload: Dict[str, Any] = Body(...)):
    try:
        with session_scope() as session:
            it = session.query(ClosetItem).filter(ClosetItem.id == int(item_id)).first()
            if not it:
                return JSONResponse(status_code=404, content={"ok": False, "error": "not found"})

            for key in ("name", "category", "color", "season", "style", "scene"):
                if key in payload and payload[key] is not None:
                    setattr(it, key, str(payload[key]).strip())
            if "isFavorite" in payload and payload["isFavorite"] is not None:
                it.is_favorite = bool(payload["isFavorite"])
            it.updated_at = now_ms()
            session.flush()
            return {"ok": True}
    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


@app.delete("/api/closet/items/{item_id}")
def closet_delete_item(item_id: int):
    try:
        with session_scope() as session:
            it = session.query(ClosetItem).filter(ClosetItem.id == int(item_id)).first()
            if not it:
                return {"ok": True}
            image_name = it.image_name
            session.delete(it)
            session.flush()

        # 尝试删除本地文件（不影响 DB 删除结果）
        try:
            p = (UPLOAD_DIR / image_name).resolve()
            if str(p).startswith(str(UPLOAD_DIR.resolve())) and p.exists():
                p.unlink()
        except Exception:
            pass
        return {"ok": True}
    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


@app.post("/api/closet/items/{item_id}/tag")
def closet_refresh_tag(item_id: int):
    start = time.time()
    try:
        with session_scope() as session:
            it = session.query(ClosetItem).filter(ClosetItem.id == int(item_id)).first()
            if not it:
                return JSONResponse(status_code=404, content={"ok": False, "error": "not found"})

            image_url = _public_file_url(it.image_name)
            try:
                if _is_public_base_url(PUBLIC_BASE_URL):
                    tag_result = _vl_tag_by_image_input(image_url)
                else:
                    raise RuntimeError("PUBLIC_BASE_URL 非公网，改用 data URL")
            except Exception:
                p = (UPLOAD_DIR / it.image_name).resolve()
                img_bytes = p.read_bytes()
                compressed = _shrink_for_vl(img_bytes)
                tag_result = _vl_tag_by_image_input(_data_url(compressed, "image/jpeg"))
            now = now_ms()
            it.tags_json = json.dumps(tag_result, ensure_ascii=False, separators=(",", ":"))
            it.tag_model = VL_MODEL
            it.tag_updated_at = now
            it.updated_at = now
            session.flush()

        elapsed = int((time.time() - start) * 1000)
        return {"ok": True, "model": VL_MODEL, "result": tag_result, "elapsed_ms": elapsed}
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e), "elapsed_ms": elapsed})


def _read_upload(file: UploadFile) -> bytes:
    return file.file.read()


def _encode_jpeg(img: Image.Image, quality: int = 92) -> bytes:
    out = io.BytesIO()
    rgb = img.convert("RGB")
    rgb.save(out, format="JPEG", quality=quality)
    return out.getvalue()

def _shrink_for_tryon(img_bytes: bytes, max_side: int = 1024) -> bytes:
    """
    DashScope aItryon 需要从公网 URL 下载图片，并做 data inspection。
    为了降低下载耗时/超时概率：在上传后先做缩放与压缩，再对外提供 URL。
    """
    img = Image.open(io.BytesIO(img_bytes))
    w, h = img.size
    side = max(w, h)
    if side > max_side:
        scale = max_side / float(side)
        tw = max(1, int(w * scale))
        th = max(1, int(h * scale))
        img = img.resize((tw, th))
    return _encode_jpeg(img, quality=90)


def _shrink_for_vl(img_bytes: bytes, max_side: int = 1024) -> bytes:
    """
    qwen-vl-plus 接收 data URL 时，为了降低体积与延迟做一次压缩/缩放。
    仅用于 fallback（当 URL 方案不可用或超时）。
    """
    img = Image.open(io.BytesIO(img_bytes))
    w, h = img.size
    side = max(w, h)
    if side > max_side:
        scale = max_side / float(side)
        tw = max(1, int(w * scale))
        th = max(1, int(h * scale))
        img = img.resize((tw, th))
    return _encode_jpeg(img, quality=85)


def _mock_tryon(person_bytes: bytes, cloth_bytes: bytes) -> bytes:
    person = Image.open(io.BytesIO(person_bytes)).convert("RGB")
    cloth = Image.open(io.BytesIO(cloth_bytes)).convert("RGB")

    pw, ph = person.size
    cw, ch = cloth.size
    target_w = int(pw * 0.55)
    scale = target_w / max(1, cw)
    target_h = int(ch * scale)
    cloth = cloth.resize((target_w, target_h))

    x = int((pw - target_w) / 2)
    y = int(ph * 0.35)  # 大致胸口位置
    overlay = Image.new("RGBA", person.size, (0, 0, 0, 0))
    overlay.paste(cloth.convert("RGBA"), (x, y))
    merged = Image.alpha_composite(person.convert("RGBA"), overlay).convert("RGB")
    return _encode_jpeg(merged)


def _is_public_base_url(url: str) -> bool:
    u = (url or "").strip().lower()
    return not (
        u.startswith("http://127.") or u.startswith("http://localhost") or u.startswith("http://10.0.2.2")
    )


def _save_upload_bytes(content: bytes, suffix: str) -> str:
    name = f"{uuid.uuid4().hex}{suffix}"
    path = UPLOAD_DIR / name
    path.write_bytes(content)
    return name


def _dashscope_submit_tryon(person_url: str, garment_url: str) -> str:
    if not DASHSCOPE_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")
    endpoint = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis/"
    headers = {
        "Authorization": f"Bearer {DASHSCOPE_API_KEY}",
        "Content-Type": "application/json",
        "X-DashScope-Async": "enable",
    }
    payload = {
        "model": "aitryon",
        "input": {
            "person_image_url": person_url,
            # 先按“上衣”传入；后续可根据衣橱类别映射 top/bottom
            "top_garment_url": garment_url,
        },
        "parameters": {
            "resolution": -1,
            "restore_face": True,
        },
    }
    last_err: Optional[Exception] = None
    for attempt in range(2):
        try:
            resp = requests.post(
                endpoint,
                headers=headers,
                data=json.dumps(payload),
                timeout=(DASHSCOPE_CONNECT_TIMEOUT_SECONDS, DASHSCOPE_READ_TIMEOUT_SECONDS),
            )
            break
        except (requests.Timeout, requests.ConnectionError) as e:
            last_err = e
            time.sleep(1.5)
    else:
        raise RuntimeError(f"DashScope 提交超时/网络失败：{last_err}")
    if resp.status_code >= 300:
        raise RuntimeError(f"DashScope 提交失败（HTTP {resp.status_code}）：{resp.text}")
    data = resp.json()
    task_id = (
        (data.get("output") or {}).get("task_id")
        or (data.get("output") or {}).get("taskId")
        or data.get("task_id")
        or data.get("taskId")
    )
    if not task_id:
        raise RuntimeError(f"DashScope 返回缺少 task_id：{resp.text}")
    return str(task_id)


def _dashscope_poll_result(task_id: str) -> str:
    if not DASHSCOPE_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")
    endpoint = f"https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}"
    headers = {"Authorization": f"Bearer {DASHSCOPE_API_KEY}"}

    deadline = time.time() + TRYON_MAX_WAIT_SECONDS
    last_text = ""
    while time.time() < deadline:
        try:
            resp = requests.get(
                endpoint,
                headers=headers,
                timeout=(DASHSCOPE_CONNECT_TIMEOUT_SECONDS, DASHSCOPE_READ_TIMEOUT_SECONDS),
            )
        except (requests.Timeout, requests.ConnectionError):
            time.sleep(1.2)
            continue
        last_text = resp.text
        if resp.status_code >= 300:
            time.sleep(1.2)
            continue
        data = resp.json()
        output = data.get("output") or {}
        status = (output.get("task_status") or output.get("taskStatus") or "").upper()
        if status in ("SUCCEEDED", "SUCCESS", "DONE"):
            results = output.get("results") or output.get("result") or []
            if isinstance(results, dict):
                results = [results]
            if isinstance(results, list) and results:
                url = results[0].get("url") or results[0].get("image_url") or results[0].get("imageUrl")
                if url:
                    return str(url)
            # 有些返回在 output.url 之类，兜底找一找
            for key in ("url", "image_url", "imageUrl"):
                if key in output:
                    return str(output[key])
            raise RuntimeError(f"DashScope 成功但未返回图片 URL：{last_text}")
        if status in ("FAILED", "FAIL"):
            raise RuntimeError(f"DashScope 生成失败：{last_text}")
        time.sleep(1.2)
    raise RuntimeError(f"DashScope 生成超时（{TRYON_MAX_WAIT_SECONDS}s）：{last_text}")


def _dashscope_tryon(person_bytes: bytes, cloth_bytes: bytes) -> bytes:
    if not _is_public_base_url(PUBLIC_BASE_URL):
        raise RuntimeError(
            "DashScope aItryon 需要公网可访问的图片 URL。请把后端部署到公网，并设置 PUBLIC_BASE_URL 为公网地址。"
        )

    # 先压缩/缩放，提高 DashScope 拉取成功率
    person_bytes = _shrink_for_tryon(person_bytes)
    cloth_bytes = _shrink_for_tryon(cloth_bytes)

    person_name = _save_upload_bytes(person_bytes, ".jpg")
    cloth_name = _save_upload_bytes(cloth_bytes, ".jpg")
    person_url = PUBLIC_BASE_URL.rstrip("/") + f"/files/{person_name}"
    cloth_url = PUBLIC_BASE_URL.rstrip("/") + f"/files/{cloth_name}"

    task_id = _dashscope_submit_tryon(person_url, cloth_url)
    result_url = _dashscope_poll_result(task_id)
    img_resp = requests.get(
        result_url, timeout=(DASHSCOPE_CONNECT_TIMEOUT_SECONDS, DASHSCOPE_READ_TIMEOUT_SECONDS)
    )
    if img_resp.status_code >= 300:
        raise RuntimeError(f"结果图片下载失败（HTTP {img_resp.status_code}）")
    return img_resp.content


def _dashscope_chat_completions(payload: Dict[str, Any]) -> Dict[str, Any]:
    if not DASHSCOPE_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")
    endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {DASHSCOPE_API_KEY}",
        "Content-Type": "application/json",
    }
    last_err: Optional[Exception] = None
    for attempt in range(2):
        try:
            resp = requests.post(
                endpoint,
                headers=headers,
                data=json.dumps(payload),
                timeout=(DASHSCOPE_CONNECT_TIMEOUT_SECONDS, DASHSCOPE_READ_TIMEOUT_SECONDS),
            )
            break
        except (requests.Timeout, requests.ConnectionError) as e:
            last_err = e
            time.sleep(1.5)
    else:
        raise RuntimeError(f"DashScope 调用超时/网络失败：{last_err}")
    if resp.status_code >= 300:
        raise RuntimeError(f"DashScope 调用失败（HTTP {resp.status_code}）：{resp.text}")
    return resp.json()


def _data_url(content: bytes, content_type: str) -> str:
    b64 = base64.b64encode(content).decode("ascii")
    return f"data:{content_type};base64,{b64}"


@app.post("/api/vl/tag")
async def vl_tag(image: UploadFile = File(...)):
    """
    用 qwen-vl-plus 对图片打标签（返回结构化 JSON）。
    App 不需要提供公网 URL，直接上传图片即可。
    """
    start = time.time()
    try:
        img_bytes = _read_upload(image)
        if not img_bytes:
            return JSONResponse(status_code=400, content={"ok": False, "error": "缺少图片内容"})

        content_type = image.content_type or "image/jpeg"
        result = _vl_tag_by_image_input(_data_url(img_bytes, content_type))
        elapsed = int((time.time() - start) * 1000)
        return {"ok": True, "model": VL_MODEL, "result": result, "elapsed_ms": elapsed}
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e), "elapsed_ms": elapsed})


@app.post("/api/vl/recommend")
async def vl_recommend(payload: Dict[str, Any]):
    """
    纯文本智能推荐：输入衣橱/天气/性别等，让模型输出“今日推荐”文案与搭配建议。
    """
    start = time.time()
    try:
        prompt = (
            "你是穿搭助手。根据输入的 weather、gender、closet_items（每件含 category/color/season/style/scene），"
            "给出今日穿搭建议。请严格只输出 JSON："
            "{title, summary, items(array of {category, reason}), tips(array)}。"
        )
        payload2 = {
            "model": VL_MODEL,
            "messages": [
                {"role": "user", "content": [{"type": "text", "text": prompt + "\n输入：" + json.dumps(payload, ensure_ascii=False)}]}
            ],
            "temperature": 0.5,
        }
        data = _dashscope_chat_completions(payload2)
        content = (((data.get("choices") or [{}])[0].get("message") or {}).get("content")) or ""
        result = _parse_llm_json(content)
        elapsed = int((time.time() - start) * 1000)
        return {"ok": True, "model": VL_MODEL, "result": result, "elapsed_ms": elapsed}
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e), "elapsed_ms": elapsed})


@app.post("/api/tryon")
async def tryon(
    personImage: UploadFile = File(...),
    clothImage: UploadFile = File(...),
):
    start = time.time()
    try:
        person_bytes = _read_upload(personImage)
        cloth_bytes = _read_upload(clothImage)
        if not person_bytes or not cloth_bytes:
            return JSONResponse(
                status_code=400,
                content={"ok": False, "error": "缺少图片内容"},
            )

        if TRYON_PROVIDER == "dashscope":
            out_bytes = _dashscope_tryon(person_bytes, cloth_bytes)
        else:
            out_bytes = _mock_tryon(person_bytes, cloth_bytes)

        elapsed = int((time.time() - start) * 1000)
        result = TryOnResult(
            ok=True,
            result_image_base64=base64.b64encode(out_bytes).decode("ascii"),
            content_type="image/jpeg",
            elapsed_ms=elapsed,
        )
        return result.__dict__
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        result = TryOnResult(ok=False, error=str(e), elapsed_ms=elapsed)
        return JSONResponse(status_code=500, content=result.__dict__)
