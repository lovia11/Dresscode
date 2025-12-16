#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
方案 A（推荐）：在打包前“预先打标”，把大模型输出写回 app/src/main/assets/outfits.json。

用法（先用启发式占位跑通流程）：
  python tools/tag_outfits.py --provider heuristic

后续接入大模型（Key 永远放在后端/脚本环境，不进 App）：
  1) 你可以把 provider 改成调用你自己的后端 HTTP 接口；
  2) 或者在这里实现某个国内模型的 API 调用（不建议把 Key 提交到 git）。

脚本默认“只补齐空字段”，不会覆盖你手填的字段；加 --overwrite 才会覆盖。
"""

from __future__ import annotations

import argparse
import json
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


ASSETS_JSON = Path("app/src/main/assets/outfits.json")


@dataclass
class TagResult:
    gender: str
    style: str
    season: str
    scene: str
    weather: str
    tags: str
    ai_tags_json: str
    model: str


def normalize_ws(s: str) -> str:
    return (s or "").strip()


def build_ai_json(result: Dict[str, Any]) -> str:
    return json.dumps(result, ensure_ascii=False, separators=(",", ":"))


def heuristic_tag(title: str, tags: str) -> TagResult:
    t = normalize_ws(title)
    k = normalize_ws(tags)
    text = f"{t} {k}"

    def pick(mapping: List[Tuple[str, str]], default: str) -> str:
        for key, value in mapping:
            if key in text:
                return value
        return default

    gender = pick([("男", "MALE"), ("女", "FEMALE"), ("unisex", "UNISEX")], "UNISEX")
    style = pick([("通勤", "通勤"), ("运动", "运动"), ("约会", "约会"), ("街头", "街头"), ("机能", "机能"), ("休闲", "休闲")], "休闲")
    season = pick([("春夏", "春夏"), ("春秋", "春秋"), ("秋冬", "秋冬"), ("春", "春"), ("夏", "夏"), ("秋", "秋"), ("冬", "冬")], "春秋")
    scene = pick([("通勤", "通勤"), ("校园", "校园"), ("约会", "约会"), ("运动", "运动"), ("出街", "出街")], "通勤")
    weather = pick([("雨", "雨"), ("晴", "晴"), ("多云", "多云"), ("冷", "冷"), ("热", "热")], "晴")

    # tags 字段保持你已有风格（用 “ · ” 分隔）
    base_tags = [p for p in [style, season, scene] if p]
    extra = []
    for kw in ["极简", "显瘦", "透气", "轻便", "叠穿", "温柔", "气质", "百搭", "防风", "实用"]:
        if kw in text:
            extra.append(kw)
    merged = base_tags + extra
    tags_out = " · ".join(dict.fromkeys(merged)) if merged else k

    ai_obj = {
        "gender": gender,
        "style": style,
        "season": season,
        "scene": scene,
        "weather": weather,
        "keywords": merged,
        "confidence": 0.2,
        "source": "HEURISTIC",
    }
    return TagResult(
        gender=gender,
        style=style,
        season=season,
        scene=scene,
        weather=weather,
        tags=tags_out,
        ai_tags_json=build_ai_json(ai_obj),
        model="heuristic-v1",
    )


def tag_with_provider(provider: str, title: str, tags: str) -> TagResult:
    if provider == "heuristic":
        return heuristic_tag(title, tags)
    raise SystemExit(f"未实现的 provider: {provider}（建议先用 --provider heuristic 跑通流程）")


def should_fill(existing: str, overwrite: bool) -> bool:
    return overwrite or normalize_ws(existing) == ""


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", default=str(ASSETS_JSON), help="outfits.json 路径")
    parser.add_argument("--provider", default="heuristic", choices=["heuristic"], help="打标提供方")
    parser.add_argument("--overwrite", action="store_true", help="覆盖已有字段（默认只补齐空字段）")
    args = parser.parse_args()

    path = Path(args.json)
    if not path.exists():
        raise SystemExit(f"找不到文件：{path}")

    items: List[Dict[str, Any]] = json.loads(path.read_text(encoding="utf-8"))
    now = int(time.time() * 1000)

    changed = 0
    for item in items:
        title = normalize_ws(str(item.get("title", "")))
        tags = normalize_ws(str(item.get("tags", "")))
        if not title:
            continue

        result = tag_with_provider(args.provider, title, tags)

        if should_fill(str(item.get("gender", "")), args.overwrite):
            item["gender"] = result.gender
        if should_fill(str(item.get("style", "")), args.overwrite):
            item["style"] = result.style
        if should_fill(str(item.get("season", "")), args.overwrite):
            item["season"] = result.season
        if should_fill(str(item.get("scene", "")), args.overwrite):
            item["scene"] = result.scene
        if should_fill(str(item.get("weather", "")), args.overwrite):
            item["weather"] = result.weather
        if should_fill(str(item.get("tags", "")), args.overwrite):
            item["tags"] = result.tags

        item["tagSource"] = "AI" if args.provider != "heuristic" else "HEURISTIC"
        item["tagModel"] = result.model
        item["aiTagsJson"] = result.ai_tags_json
        item["tagUpdatedAt"] = now
        changed += 1

    if changed == 0:
        print("没有可更新的条目。")
        return 0

    path.write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"已更新 {changed} 条穿搭：{path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

