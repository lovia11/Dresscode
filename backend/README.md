## DressCode Backend（AI 网关 + 公网数据库）

目标：把大模型/try-on 的 Key 留在后端，提供：
- 给 Android 调用的 HTTP API（上传图片、换装、打标签、推荐）；
- 把“衣橱图片 + 标签 + 元数据”写入数据库（本地可用 SQLite，部署可用 MySQL）；
- 部署到公网后，给 DashScope 提供可访问的图片 URL（`/files/{name}`）。

### 1. 安装依赖

在仓库根目录执行：

```bash
python -m venv .venv
.\.venv\Scripts\activate
pip install -r backend/requirements.txt
```

### 2. 启动服务

```bash
uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

健康检查：打开 `http://127.0.0.1:8000/health`

### 3. 数据库（本地 SQLite / 云端 MySQL）

默认 `DATABASE_URL=sqlite:///backend_data/dresscode.db`，启动时会自动建表。

如果要用 MySQL（推荐用于“部署到公网 + 让老师验收”）：

1) 先创建数据库（建议 `utf8mb4`）。
2) 配置环境变量：

```bash
set DATABASE_URL=mysql+pymysql://user:pass@host:3306/dresscode?charset=utf8mb4
```

你也可以用 `backend/schema.mysql.sql` 手动建表；或直接让服务启动时自动建表（课程项目足够）。

注意：大模型/云服务通常 **不能直接连你的数据库**。正确方式是：后端查询 DB → 组织输入 → 调用模型；
图片则通过公网 URL（例如 `https://你的域名/files/xxx.jpg`）提供给模型拉取。

### 3. try-on 接口

`POST /api/tryon`（multipart/form-data）

- `personImage`：人像图
- `clothImage`：服装图（建议来自“衣橱收藏”的衣物照片）

返回 JSON：

- `ok`：是否成功
- `resultImageBase64`：输出图片 base64（不含 data: 前缀）
- `contentType`：如 `image/jpeg`
- `error`：失败原因（如果 ok=false）

### 4. 阿里云 DashScope aItryon（推荐）

默认后端使用 `TRYON_PROVIDER=mock`（本地占位合成，便于先跑通端到端）。

如果要调用阿里云 `aitryon`：

```bash
set TRYON_PROVIDER=dashscope
set DASHSCOPE_API_KEY=你的Key
```

重要：DashScope 的 `aitryon` 入参是 **图片 URL**。因此后端必须部署到公网，并设置：

```bash
set PUBLIC_BASE_URL=https://你的公网域名或IP
```

后端会把上传的图片临时保存到 `backend_data/uploads/`，并通过 `GET /files/{name}` 对外提供访问，再把这些 URL 传给 DashScope。

建议：在公网部署时让 Nginx 直接静态托管 `/files/`（比 Python 读文件更快，能减少 DashScope 下载超时）。示例：

```nginx
location /files/ {
  alias /var/lib/dresscode/uploads/;
}
```

### 6. DashScope 超时参数（避免 60s 超时）

部分网络环境下，DashScope 生成/打标可能超过默认 60 秒。可以在环境变量里调大：

```bash
set DASHSCOPE_CONNECT_TIMEOUT_SECONDS=10
set DASHSCOPE_READ_TIMEOUT_SECONDS=180
```

### 5. 衣橱入库接口（给“老师要求”的那部分）

- `POST /api/closet/items`（multipart）：`image` + `owner/name/category/...`，可选 `autoTag=true` 自动打标并入库
- `GET /api/closet/items?owner=xxx`：查询某个用户的衣橱列表（含 tags）
- `PATCH /api/closet/items/{id}`：更新字段/收藏状态
- `DELETE /api/closet/items/{id}`：删除
- `POST /api/closet/items/{id}/tag`：重新打标并更新数据库
