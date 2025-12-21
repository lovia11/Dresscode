
# Codex 项目速览（DressCode）

> 每次启动 Codex 先阅读此文档，了解目标、现状与下一步。修改后请提交到 git，保持单一事实来源。

## 项目简介
- 课程大作业：DressCode Android APP，类微信底部 Tab，提供穿搭展示、天气、智能换装、个人设置等。
- 语言/架构：Java + MVVM。
- 组件：ViewModel/LiveData/ViewBinding/Room；单 Activity + 多 Fragment。
- 离线可运行：必要数据可预置到 Room，远端可选自建后台。

## 功能要求（来源：实习-最终大作业-功能确认单）
- 登录/注册页面。
- 底部 Tab 切换多个核心模块，采用 MVVM。
- 天气：定位当前城市，显示天气；可切换城市。
- 穿搭展示：至少 1 页瀑布流/网格展示；按性别、风格、天气、季节、场景等筛选；搜索入口；收藏穿搭。
- 智能换装：上传/拍摄人像；选择收藏的穿搭，调用大模型换装并展示结果；可重拍替换。
- 数据库：使用本地或远程数据库（Room 等）。
- 若无 DB，可用静态数据。
- 后台：鼓励有远程后台（阿里云）。
- 后台页面/服务/DB 可公网访问。
- 设置/我的：性别设置影响穿搭展示。
- 默认筛选（风格/季节/场合等）；展示收藏、其他设置。
- 其他要求：git 多次 commit。
- Android 10 与 12 设备测试；可加自定义功能。

## 技术栈与约束
- 前端：Java、AndroidX、Navigation、RecyclerView、Glide、Retrofit2/OkHttp3、FusedLocationProvider、CameraX/系统相机。
- 本地存储：Room（User/Outfit/Favorite/City/Weather/SearchHistory/SwapJob 等），DataStore/SharedPreferences 存 Token、性别、默认筛选。
- 远端：REST API（Spring Boot + MySQL/OSS 为例）。
- 如无远端，需预置 JSON 导入 Room。
- 设计：微信式底部 Tab，浅色主题；保持可读性与移动端适配。

## 当前状态（请每次更新）
- 日期：2025-12-21
- 已完成：登录/注册（本地持久化）；衣橱模块（拍照/选图新增、列表展示、删除）；穿搭展示（列表/筛选/搜索、收藏）；收藏星标区分未收藏（灰/透明）与已收藏（蓝）；换装流程（选人像+选收藏穿搭，占位生成结果）；换装历史（本地保存、可删除）；“我的”页昵称/头像等基础信息；本地数据均按用户隔离。
- 已完成（天气）：切换到高德 Web 服务 API（逆地理编码 + 实况天气）；城市下拉可切换；点击“定位当前城市”会请求定位并刷新天气；缓存上次天气快照。
- 进行中：定位体验（模拟器/热点环境下定位不稳定时，需要手动设置模拟器 Location，或在真机开启高精度定位）；天气 UI 的“定位成功后一定切换到杭州/当前城市”依赖设备真实位置。
- 已完成（后端基础）：FastAPI AI 网关支持 DashScope（aitryon + qwen-vl-plus）；新增“衣橱入库”API，并支持用 `DATABASE_URL` 写入 SQLite/MySQL（用于部署公网数据库与验收）。
- 已完成（公网部署）：阿里云轻量应用服务器（Ubuntu）已部署后端并对外提供访问（`http://121.40.80.52/health`）；`POST /api/closet/items` 上传图片可入库并生成公网 `imageUrl`（`/files/{name}`）。
- 已完成（打标联网）：后端对 DashScope 增加更长超时与兜底策略后，`autoTag=true` 可稳定打标；衣橱新增会“先 AI 识别 → 用户确认/可修改 → 保存”。
- 已完成（换装）：Nginx 静态托管 `/files/` + 后端压缩输入图后，DashScope `aitryon` 可用；App 换装支持“收藏衣橱衣物”与“收藏穿搭（封面转图片）”两种素材。
- 已完成（首页推荐）：小技巧只显示正文（去掉“根据当前天气…”等废话）；今日推荐不显示总结卡片，仅展示推荐衣物卡片。
- 已完成（穿搭 AI 自动打标）：穿搭封面图会后台调用 `/api/vl/tag` 自动生成标签并回写 `title/tags/gender/style/.../aiTagsJson`，卡片无需手填。
- 待做：把“穿搭图片”补齐（放 `app/src/main/res/drawable/`，并在 `app/src/main/assets/outfits.json` 里填 `cover`）；清空应用数据/卸载重装以触发重新 seed；后续可把图片改为 OSS(https) 以增强 DashScope 拉取稳定性；补齐截图与 git commit 记录整理。
- 已知问题/风险：Gradle 有时会因 `C:\Users\LEGION\.gradle\wrapper\dists` 锁/权限导致构建失败；个别环境网络/DNS 会导致第三方天气域名解析失败（已优先采用国内高德接口）；编辑文件时需保持 UTF-8（避免 BOM/乱码）。

### Changelog（简要）
- 2025-12-15：天气改用高德（adcode 查询），修复“天气获取失败：OK/无数据”误报；修复定位回调导致的 `Fragment not attached` 崩溃；定位失败时使用当前下拉城市作为兜底。
- 2025-12-16：衣橱新增“场景”标签并支持编辑；Room 升级到 v6 并提供 5→6 迁移（`closet_items.scene`）。
- 2025-12-16：穿搭数据结构增加 AI 打标元数据字段（`tagSource/tagModel/aiTagsJson/tagUpdatedAt`），并提供 `tools/tag_outfits.py` 用于打包前预打标（方案A）。
- 2025-12-16：衣橱支持收藏衣物（星标）；换装页可选择“收藏穿搭”或“收藏衣橱衣物”作为换装素材；Room 升级到 v8 并提供 7→8 迁移（`closet_items.isFavorite`、`swap_jobs.source*`）。
- 2025-12-16：新增本地后端 `backend/`（FastAPI）提供 `/api/tryon`；Android 换装页优先对“收藏衣橱衣物”调用后端生成结果并写入历史。
- 2025-12-16：后端 try-on 切换为可选 DashScope `aitryon`（远程异步任务轮询）；新增 qwen-vl-plus 预留接口（`/api/vl/tag`、`/api/vl/recommend`）。
- 2025-12-16：后端新增数据库持久化（SQLite/MySQL），提供衣橱上传/查询/删除/打标接口（满足“图片+标签入库并可公网验收”的目标）。
- 2025-12-16：阿里云轻量服务器（Ubuntu）部署后端到公网 IP `121.40.80.52`，Nginx 80 端口反代到后端；MySQL 用户改为 `mysql_native_password` 以兼容 PyMySQL。
- 2025-12-18：Nginx 静态托管 `/files/` + 后端压缩输入图，解决 DashScope `DataInspection` 下载超时，换装链路跑通。
- 2025-12-18：首页推荐体验调整：小技巧仅保留正文并去空行；今日推荐只展示推荐衣物卡片。
- 2025-12-18：穿搭/衣橱接入打标：穿搭封面后台自动打标并回写卡片字段；衣橱新增先 AI 识别再确认保存。
- 2025-12-21：修复穿搭详情页 meta 分隔符显示异常；穿搭封面图根据资源名自动选择裁剪方式（照片 centerCrop）；`.gitignore` 忽略 `backend.zip`。

## 运行与调试
- 构建：./gradlew assembleDebug
- 安装调试：./gradlew installDebug
- 运行单元/仪器化测试（若有）：./gradlew test / ./gradlew connectedAndroidTest
- 开发时如需 Mock：在 docs/api-mock.md 记录 Mock 地址与示例。

## 服务器登录与运维（当前：无域名，使用公网 IP）
- 服务器公网 IP：`121.40.80.52`
- 登录（本机 PowerShell）：`ssh root@121.40.80.52`
- 安全退出：输入 `exit`（若当前在虚拟环境，可先输入 `deactivate` 再 `exit`）
- 后端服务：`systemctl status dresscode-backend --no-pager` / `systemctl restart dresscode-backend`
- 查看日志：`journalctl -u dresscode-backend -n 200 --no-pager`
- Nginx：`systemctl status nginx --no-pager` / `nginx -t && systemctl reload nginx`
- 健康检查：`curl http://121.40.80.52/health`

## 目录与约定（如有变化请更新）
- app/ 主工程；单 Activity（MainActivity）。
- Fragments：Home（天气+推荐+技巧）、Outfits（搜索/筛选/列表）、Swap（换装流程占位）、Profile（我的/设置占位）。
- data/local：Room DB 与 DAO（待建）。
- data/remote：Retrofit API；repository 作为数据源入口（待建）。
- ui/ Activity/Fragment/Adapter；viewmodel/ 各模块 VM（待建）；model/ 数据模型。
- 资源：预置穿搭 JSON/城市列表放 assets/（待添加）。

## 后端/接口（示例约定）
- 认证：POST /auth/login、/auth/register 返回 token。
- 穿搭：GET /outfits?gender=&style=&season=&scene=&weather=&page=。
- 收藏：POST /favorites，DELETE /favorites/{id}。
- 天气：GET /weather?lat=&lon=（可透传第三方）。
- 智能换装：POST /swap 上传人像+outfitId，返回 jobId。
- 换装结果：GET /swap/{jobId} 轮询状态/结果。
- 城市：GET /cities?q=；也可内置表。
- 所有需鉴权接口在 Header 携带 token。

## 数据模型草案
- User(id, phone/email, gender, token, createdAt)
- Outfit(id, title, imageUrl, gender, style, season, scene, weatherTag, desc)
- Favorite(id, userId, outfitId, createdAt)
- City(id, name, code, lat, lon, isDefault, updatedAt)
- Weather(id, cityId, temp, desc, icon, aqi, updatedAt)
- SearchHistory(id, keyword, createdAt)
- SwapJob(id, userId, outfitId, inputImageUrl, resultImageUrl, status, createdAt)

## 待办模板（示例）
- 登录/注册表单与鉴权持久化
- 底部导航 + Fragment 框架
- 天气定位与城市切换 + 缓存
- 穿搭列表/筛选/搜索 + 收藏
- 智能换装流程（拍摄/上传/换装/展示/重试）
- 设置页（性别/默认筛选）联动穿搭
- 预置数据导入 Room，离线可用
- 测试：Android 10、12 流程跑通；截图留存
- git 多次 commit 记录

## 协作/使用说明
- 新开会话前请让我先阅读本文件和 README。
- 开发结束后更新"当前状态"和"待办"，必要时追加简短 Changelog。
- 若引入新接口/Mock/数据结构，请同步到本文件或子文档。

## 大模型/换装选型备注
- 目标：后续接入服饰试穿大模型，优先选用 aiTryOn 服务（支持人像+服装图真实试穿）。
- 前端流程：上传/拍照人像 -> 选收藏/上传服装 -> 后端调用 aiTryOn -> 轮询/回调结果 -> 展示。

## aiTryOn plan
- Goal: integrate aiTryOn virtual try-on (person + garment) later.
- Flow: upload/capture person -> select saved/local garment -> backend calls aiTryOn -> poll/callback -> show result.
- Security: API key stays on backend only; document call/auth details in docs/api.md when integrating.

codex resume 019b20c3-1ab3-7080-b0e1-c7e9bc8ca74a
codex resume 019b20c3-1ab3-7080-b0e1-c7e9bc8ca74a
