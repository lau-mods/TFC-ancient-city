# TFC Ancient City 仕様

Vanilla Ancient CityをTerraFirmaCraft環境へ統合し、構造物内の一部ブロックとlootをTFC相当に置き換えるMinecraft 1.21.1 / NeoForge向けModである。

## 共通開発ルール

- 仕様ファイル名は大文字の `AGENTS.md` に統一する。
- 本書は現在維持すべき仕様を記録し、挙動、対応バージョン、依存関係、ID、登録内容、有効化条件、生成規則、検証手順の変更と同時に更新する。
- READMEは利用者向けの短い概要、配布先、build入口に絞り、詳細仕様を重複掲載しない。
- ライセンスと第三者表示はrootの `LICENCE` 一つへ統合し、別のlicense/noticeファイルを作らない。
- 現在値は `gradle.properties`、Gradle設定、Mod metadata、実装コード、同梱dataを正本とし、別バージョンの記憶ではなく対象版の公式ソース、依存ソース、実JAR/dataで確認する。
- 公開API、registry、tag、dataを優先し、Mixinは必要な対象へ限定する。client専用クラスをcommon/server側から参照せず、公開済みIDはworld/data pack互換性を優先する。
- 依存JAR、展開物、解析・生成scriptは `.tmp/` に置いてGit管理外にし、BOMなしUTF-8のJSONを使う。
- `.gradle/`、`build/`、`runs/`、IDE metadata、依存JARを変更対象に含めず、無関係な既存差分を編集しない。
- 変更前に既存の登録、命名、resource配置を確認し、依頼外のrename、format変更、依存・version更新を混ぜない。
- 通常は `./gradlew compileJava`、完了時は `./gradlew build` を実行する。data変更時は全JSONをparseし、Mixin変更では対象構造物外への影響と専用サーバー安全性を確認する。
- Minecraftクライアントはランタイム確認が必要な変更または明示依頼時だけ起動し、未実施の検証は理由と範囲を報告する。

## 基本情報

- Mod IDは `tfcancientcity`、Java packageは `net.claustra01.tfcancientcity`。
- Minecraftは `1.21.1`、NeoForgeは `21.1.219`、Javaは `21`。
- TFCを必須依存とし、versionの現在値は `gradle.properties` を正とする。

## Ancient City置換

- processor typeは `tfcancientcity:tfc_block_replacement`。
- `StructureTemplateManager#getOrCreate` でtemplate IDを保持し、`StructureTemplate#placeInWorld` の先頭でprocessorを末尾へ追加する。
- 対象は `minecraft:ancient_city/*` のtemplateだけとし、同じsettingsへ同一processorを重複追加しない。
- 置換mapはAncient City内に実在し、明確なTFC対応先があるVanilla blockへ限定する。Dark Oak系はTFC Blackwood、chest・lecternもBlackwood、candle・torchはTFC同等品、iron trapdoorはWrought Ironへ置換する。
- furnace、campfire、soul campfire、smoker、blast furnaceはTFC firepitへ置換し、互換性のないblock entity NBTを破棄する。
- 置換元と先に同名・同値propertyがある場合はstateを引き継ぎ、firepitは元のhorizontal facingからaxisを設定する。
- 置換先registry IDが存在しない場合は元blockを維持し、対象IDごとに一度だけwarningを出す。

## 独自data

- `tfcancientcity:rock/loose/deepslate` と `tfcancientcity:brick/deepslate` を登録し、TFC/Common tag、recipe、model、textureを維持する。
- Ancient Cityのbiome tag、chest loot table、deepslate block loot overrideは同梱dataを正とし、Vanilla 1.21.1とTFC対象版の実dataを比較して変更する。
- 他namespaceのresourceは、Vanilla/TFCへ意図して追加・上書きする互換dataだけに使う。
