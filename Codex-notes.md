
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
- 日期：2025-12-11
- 已完成：单 Activity + BottomNav + Navigation 框架；首页布局含天气卡片 + 今日推荐横向列表 + 穿搭技巧卡片；穿搭页布局含搜索、筛选 Chip、列表占位；换装/我的占位页；依赖加入 Navigation/Lifecycle/Room/Retrofit/OkHttp/RecyclerView；ViewBinding 开启。
- 进行中：暂无（待接数据/逻辑）。
- 待做：接入天气 API + 定位；穿搭列表数据源（本地 Room 预置 + 远端可选），收藏与筛选逻辑；换装流程（上传/选图 -> 调用后端/占位生成）；登录/注册与用户偏好（性别/默认筛选）；Room 模型与 Repository；预置数据导入；测试与截图。
- 已知问题/风险：此前 Gradle 下载有权限锁（`C:\Users\LEGION\.gradle\wrapper\dists`），如再遇需清理锁或以有权限账号构建；尚未初始化 git 仓库；当前数据为占位，需尽快填充避免后续大改。

## 运行与调试
- 构建：./gradlew assembleDebug
- 安装调试：./gradlew installDebug
- 运行单元/仪器化测试（若有）：./gradlew test / ./gradlew connectedAndroidTest
- 开发时如需 Mock：在 docs/api-mock.md 记录 Mock 地址与示例。

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