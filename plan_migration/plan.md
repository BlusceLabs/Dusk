<!-- EXECUTION PROTOCOL (每轮必读)
1. file_read(plan.md), find first [ ] item
2. If step has SOP -> file_read that SOP's key section
3. Execute step + Mini verify
4. file_patch mark [ ] -> [✓]+brief result, then back to step 1
5. All steps done -> termination check: file_read(plan.md) confirm 0 [ ] remaining
⚠ No memory-execution | No skipping verify | No termination without check
💡 Heavy lifting (reading lots of files/web/repetitive) -> delegate to subagent
-->

# Dusk: Expo -> Native Android (Kotlin+Java+Compose) Migration

**需求**: 将 Dusk 从 Expo/React Native 迁移到 Kotlin+Java + Jetpack Compose 原生 Android
**约束**: 保留 Firebase 后端 + Node.js API 服务器；核心功能优先（Feed/Auth/Profile/DMs）；逐步剥离旧代码

## 探索发现
- **来源**: code_run + file_read on /home/mantra/Dusk/artifacts/
- **代码结构**: `artifacts/dusk/` = Expo app (TSX), `artifacts/api-server/` = Node.js 后端（保留）
- **Firebase**: 在 `lib/firebase/` 使用 Firebase Auth（email/password + Google）、Firestore、Storage
- **认证**: email/password 注册登录、username 登录、Google Sign-In（Credential Manager）
- **Firestore 集合**: users, posts, notifications, liveStreams, following, likes, reposts, bookmarks, polls, communities, chat, stories, usernames
- **数据模型**: User, Post, Poll, Conversation, Message, Community, Notification, Stream, Story
- **状态管理**: Zustand store + React Context（AppContext）
- **导航**: Expo Router（tabs + stack），5个底部标签（Home/Search/Reels/Notifications/Profile）
- **核心功能**: 动态Feed（For You/Following/Live）、Stories、Reels、直播、社区、私信、通知、个人资料、搜索、打赏/付费
- **支付**: PayPal 集成（通过 API 服务器）
- **CI/CD**: build-apk.yml（GitHub Actions）构建 APK
- **不确定点**: google-services.json 是否包含有效凭证（后续检查）；旧代码剥离时机

## 执行计划

### Phase 0: Android Project Scaffold
1. [✓] 创建 Android 项目根结构（build.gradle.kts, settings.gradle.kts, gradle wrapper, libs.versions.toml）
2. [✓] 创建 `app/` 模块（build.gradle.kts, AndroidManifest.xml）
3. [✓] 配置依赖版本目录（Firebase BoM, Compose BOM, Hilt, Retrofit, Coil, etc）
4. [✓] 创建 Application 类（Hilt entry point + Firebase init）
5. [✓] 创建基础包结构（data/, domain/, ui/, di/, navigation/）
6. [✓] 迁移 google-services.json + 配置 Firebase
7. [✓] **[VERIFY] Phase 0 验证** -- 28 files verified (root build, gradle config, app module, AndroidManifest, package structure)

### Phase 1: Core Foundation
8. [✓] 创建 Material Theme + 颜色系统（从 constants/colors.ts 移植）- Color.kt, Theme.kt, Type.kt 完成
9. [✓] 创建 Auth Repository（Firebase Auth 封装：email/password, Google Sign-In, username login）
10. [✓] 创建 Firestore Repository（users, posts 集合 CRUD + 实时监听 Flow）
11. [✓] 创建 API Service（Retrofit 封装 Node.js API）
12. [✓] 创建 Navigation 框架（NavHost + Auth Gate + Bottom Nav Bar）
13. [✓] 创建 Auth UI Screens（SignIn, SignUp, ForgotPassword）+ AuthViewModel
14. [✓] **[VERIFY] Phase 1 验证** -- 23 Kotlin files total, 17 key files verified (repos, API, DI, nav, auth screens, theme)

### Phase 2: Core Features
15. [✓] 创建 Feed Screen（For You / Following / Live tabs + PostCard 组件）
16. [✓] 创建 Story Bar 组件
17. [✓] 创建 Profile Screen（header, stats, content tabs, edit）
18. [✓] 创建 Chat / DM 界面（conversation list + chat detail + real-time）
19. [✓] 创建 Search & Explore 界面
20. [✓] **[VERIFY] Phase 2 验证** -- 71 files total, 34 UI screens/composables, all routes wired in NavGraph, repos bound in AppModule

### Phase 3: Content Features
21. [x] 创建 Reels / 短视频界面
22. [x] 创建 Notifications 界面
23. [x] 创建 Communities 界面
24. [x] 创建 Stories viewer + editor
25. [x] 创建 Live Streaming viewer + go-live
26. [x] **[VERIFY] Phase 3 验证**

### Phase 4: Premium & Cleanup
27. [x] 创建 Premium / Subscription screens
28. [x] 集成 PayPal 支付
29. [x] 创建 Wallet 界面
30. [x] 剥离旧 Expo 代码（删除 artifacts/dusk/）
31. [x] 更新 CI/CD workflow（build-apk.yml -> Gradle）
32. [x] **[VERIFY] 最终验证 + 终止检查**
