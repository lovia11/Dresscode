DashScope integration notes (ASCII-only doc)

This repo uses a local FastAPI backend as an AI gateway so Android never stores the API key.

1) aItryon (image2image, async)
- Endpoint: `https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis/`
- Headers: `Authorization: Bearer $DASHSCOPE_API_KEY`, `X-DashScope-Async: enable`
- Poll: `GET https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}`

Important: aItryon input fields are URLs. If you run backend locally, DashScope cannot fetch your `http://127.0.0.1/...` images.
Deploy backend to a public server and set:
- `TRYON_PROVIDER=dashscope`
- `DASHSCOPE_API_KEY=...`
- `PUBLIC_BASE_URL=https://your-public-domain-or-ip`

2) qwen-vl-plus (tagging / recommendation)
Backend provides:
- `POST /api/vl/tag` (multipart image) -> returns JSON tags
- `POST /api/vl/recommend` (JSON input) -> returns JSON recommendation

qwen-vl-plus calls are implemented via DashScope OpenAI-compatible endpoint:
`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`

DB note:
- Cloud models do NOT connect to your MySQL directly.
- Correct flow is: your backend queries DB + hosts images under a public URL, then calls DashScope with those URLs / derived text.
