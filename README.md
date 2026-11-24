# MITE: Rebuilt

MITE: Rebuilt 是面向 Minecraft Java 版 1.20.1 的 Forge 生存模组，复刻经典 MITE 的生存机制，并提供可选的扩展内容。

## 安装

使用 Minecraft Java 版 **1.20.1**、Forge **47.4.28** 和 Java **17**。从 [Releases](../../releases) 下载 **mite-rebuilt-0.4.26.jar**，放入 Forge 实例的 mods 目录。多人联机时，服务端和客户端使用同一版本。JEI 和 Inventory Sorter 随 JAR 打包。

## 从源码打包

安装 JDK 17 后，在仓库根目录运行：

| 系统 | 命令 |
| --- | --- |
| Windows | gradlew.bat jarJar |
| Linux / macOS | ./gradlew jarJar |

Gradle 将构建产物写到 build/libs。首次构建需要下载 Gradle、Forge 和 JEI 依赖。Release 中的 JAR 来自原项目的已验证发行流程，仓库里的 Gradle 入口用于独立源码构建。

## 第三方许可

MIT 适用于本项目独立创作的代码。上游素材、衍生成果及内嵌依赖保留各自的许可和版权声明，详见 [LICENSE](LICENSE)、[第三方声明](THIRD_PARTY.md) 和 [拷贝随附的声明](src/main/resources/META-INF/licenses)。内嵌的 Inventory Sorter 附有[对应上游源码](third-party/inventorysorter-23.0.11-sources.zip)。
