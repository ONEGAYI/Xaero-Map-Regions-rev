# Xaero Map Regions Rev 项目规则

## 项目目标

本项目面向 Minecraft 1.20.1 Forge，从零实现一个与 Xaero Map Regions 功能相近的区域标注模组。目标是先完成稳定 MVP，再逐步补齐图形化编辑、自定义图标、已探索区域裁剪、进入区域标题等高级能力。

## 技术约定

- 目标版本：Minecraft 1.20.1 + Forge 47.3.33。
- 向上兼容：Forge 版本号集中放在构建属性中；Forge 与 Xaero 接入细节集中在适配层，业务逻辑不直接散落依赖版本细节。
- 主要语言：Java。
- Xaero's World Map 作为外部依赖使用，不打包进本项目产物。
- 服务端作为区域数据权威来源，客户端只缓存和渲染同步数据。
- 区域编辑权限默认限制为 OP 且处于创造模式的玩家。
- 实现功能或修复问题前优先补充契约测试，能用纯 Java 单元测试覆盖的逻辑不要依赖游戏运行环境。
- 本地烟测使用 `./gradlew clean test build` 和 `./gradlew runClient`；运行期 Xaero World Map jar 放在 `run/mods/`，该目录不进入 Git。

## 文档与提交

- 设计、计划、CHANGELOG、提交信息和 PR 描述默认使用中文。
- 通用项目规则维护在本文件。
- `CLAUDE.md` 只保留 Claude 专属补充规则，并通过 `@AGENTS.md` 引入本文件。
- 修改文件树或模块职责后，同步维护下方文件树说明。

## 文件树

```text
.
├── AGENTS.md
│   项目通用规则、技术约定与文件树说明。
├── CLAUDE.md
│   Claude 专属入口文件，通过 @AGENTS.md 引入通用规则。
└── docs/
    └── superpowers/
        ├── plans/
        │   └── 2026-07-02-xaero-map-regions-mvp-implementation.md
        │       Forge 47.3.33 区域标注 MVP 实现计划。
        └── specs/
            └── 2026-07-02-xaero-map-regions-mvp-design.md
                Forge 1.20.1 版 Xaero 区域标注 MVP 设计文档。
```
