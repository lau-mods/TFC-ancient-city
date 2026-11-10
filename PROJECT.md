# Project

Status: active

Project ID: tfc-ancient-city
Mod ID: tfcancientcity

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.264
Java: 21

## Purpose

TFC Ancient City v1.0 は、Minecraft 1.21.1 の Vanilla Ancient City を TerraFirmaCraft（以下 TFC）の地形・資源・加工体系へ統合する NeoForge アドオンである。古代都市の構造、Deep Dark の機能、Warden と Sculk の挙動、生成条件、既存の探索報酬を維持し、構造物内の限定的なブロック置換と深層岩の TFC 式採掘・加工機能を実装する。

登録名前空間は `tfcancientcity`、Java の基本パッケージは `net.claustra01.tfcancientcity`、製品バージョンは `1.0.0` とする。Minecraft の対応バージョンは 1.21.1 のみとする。NeoForge 21.1.264 は再設計の検証基準であり、Minecraft 1.21.1 に対応する NeoForge・TFC の更新には継続対応する。他の Minecraft バージョンへの移植は本プロジェクトの対象外とする。TFC は必須依存とし、TFC Overdims は参照実装であって必須依存にはしない。

本書だけで実装判断が完結することを前提とし、過去の会話や別途作成された仕様書の参照を要求しない。既存コードの継承部分と新規実装の境界は以下の固定参照で決定する。

| 用途 | 固定参照 | 参照対象 |
| --- | --- | --- |
| 既存機能・戦利品・バイオーム・ブロック置換 | [TFC Ancient City `v0.1.1`](https://github.com/lau-mods/TFC-ancient-city/tree/7e0f43a9510a844607e480c023aec15ff7d8062d) | Java、JSON、登録 ID、資産 |
| 同リポジトリの新しい開発規約 | [TFC Ancient City `main` の `AGENTS.md`](https://github.com/lau-mods/TFC-ancient-city/blob/28191e4009228966170b4d2529be238fcac50543/AGENTS.md) | コード・データと矛盾しない運用規則 |
| TFC 式石材処理 | [TFC Overdims `v0.1.4`](https://github.com/lau-mods/TFC-Overdims/tree/8c8aa4e7594cbb692dace707c870299fa6aa7d44) | `LooseRockBlock`、石材登録、採掘 loot、レシピ、タグ、GameTest |
| 開発ハーネスの形式 | [modding-harness `PROJECT.md` テンプレート](https://github.com/lau-mods/modding-harness/blob/d1e3524b9574966ca84a38fab7af0fb8bc0520e3/templates/PROJECT.md) | Feature、Acceptance Criteria、検証形式 |

**仕様の優先順位**は、本書の v1.0 固有規則、TFC Ancient City v0.1.1 の実コード・同梱データ、TFC Overdims v0.1.4 の該当機能の実コード・同梱データ、各リポジトリの `AGENTS.md`・`PROJECT.md`・README の順とする。ただし、依存先の公開 API・実際のレジストリ・ロード結果が異なる場合はその差異を検証して解消する。開発文書の記述だけでコードの挙動を推定しない。例えば旧 `AGENTS.md` にある焚き火から Firepit への置換は、v1.0 では明示的に廃止する。

## Features

### F-001: 対応環境と MOD 登録

#### Description

Minecraft 1.21.1、Java 21、NeoForge 21.1.264 を初期検証構成とする。`mod_id=tfcancientcity`、`mod_version=1.0.0` とし、TFC を required dependency としてサーバー・クライアント双方に適用する。TFC 4.x 系の Minecraft 1.21.1 対応版を検証対象とし、初期比較には Overdims v0.1.4 が参照する TFC 4.2.12 の API・データを利用する。依存先更新に伴う内部実装の変更は許容するが、ここに記載した ID とゲーム挙動は変更しない。

新規 MOD への分離や他 MOD のインストールは要求しない。既存のアイテム ID `tfcancientcity:rock/loose/deepslate`、`tfcancientcity:brick/deepslate`、プロセッサ ID `tfcancientcity:tfc_block_replacement` を維持する。旧ワールドとの互換性自体は保証しない。

#### Acceptance Criteria

##### AC-F001-001: サーバーとクライアントの起動

Preconditions:
Minecraft 1.21.1、Java 21、NeoForge 21.1.264、対応する TFC と本 MOD があり、TFC Overdims は導入されていない。

Action:
専用サーバーとクライアントを起動し、ログ、MOD 一覧、`tfcancientcity` の登録内容を確認する。

Expected Result:
欠落した必須依存やレジストリ衝突による失敗なく起動し、`tfcancientcity` が読み込まれる。TFC Overdims を追加導入せずに全ての本 MOD 固有登録が存在する。専用サーバーからクライアント専用クラスを参照しない。

##### AC-F001-002: 対応範囲と既存 ID

Preconditions:
ビルド済みの MOD と、その metadata・登録一覧がある。

Action:
Minecraft の依存バージョン範囲、MOD ID、製品バージョン、既存 3 ID と TFC の依存宣言を検査する。

Expected Result:
Minecraft 1.21.1 のみを対象とし、バージョンは `1.0.0`、MOD ID は `tfcancientcity` である。上記既存 3 ID が維持され、TFC は必須、TFC Overdims は非必須である。

### F-002: 古代都市の生成と基本挙動

#### Description

Vanilla の `minecraft:ancient_city` を使用し、構造物・テンプレート・ジグソープール・建物配置・生成高度・間隔・生成頻度を変更しない。新規構造物、Deep Dark 相当の新規地下バイオーム、追加の Warden 調整を実装しない。Sculk、Sensor、Shrieker、Catalyst、強化された深層岩、羊毛・カーペット、レッドストーン・隠し回路はバニラのまま扱う。

`data/minecraft/tags/worldgen/biome/has_structure/ancient_city.json` は v0.1.1 と同じ `replace: true`、以下の **33 種類と完全一致する値集合**を保持する。バイオームの選定を自動計算に変更しない。

```text
tfc:burren_plateau
tfc:cenote_highlands
tfc:cenote_plateau
tfc:doline_highlands
tfc:doline_plateau
tfc:extreme_doline_mountains
tfc:extreme_doline_plateau
tfc:glacially_carved_mountains
tfc:glacially_carved_oceanic_mountains
tfc:glaciated_mountains
tfc:glaciated_oceanic_mountains
tfc:highlands
tfc:ice_sheet_mountains
tfc:ice_sheet_mountains_edge
tfc:ice_sheet_oceanic_mountains
tfc:ice_sheet_oceanic_mountains_edge
tfc:mountain_lake
tfc:mountains
tfc:oceanic_mountain_lake
tfc:oceanic_mountains
tfc:old_mountain_lake
tfc:old_mountains
tfc:plateau
tfc:plateau_lake
tfc:plateau_wide
tfc:rocky_plateau
tfc:shilin_highlands
tfc:shilin_plateau
tfc:tower_karst_highlands
tfc:volcanic_mountain_lake
tfc:volcanic_mountains
tfc:volcanic_oceanic_mountain_lake
tfc:volcanic_oceanic_mountains
```

Ancient City の構造物以外へ Loose Deepslate を自然配置する worldgen feature、地下地形加工、下面硬化処理は追加しない。

#### Acceptance Criteria

##### AC-F002-001: 生成バイオーム定義

Preconditions:
本 MOD のデータがロードされ、TFC のバイオームレジストリが存在する。

Action:
`minecraft:has_structure/ancient_city` の元となる JSON とロード後のタグ、`minecraft:ancient_city` の生成設定を検査する。

Expected Result:
JSON は `replace: true` であり、上記 33 種類以外のバイオームを追加・削除しない。構造物の生成間隔・配置・高度・テンプレート構成について、本 MOD 由来の変更が存在しない。

##### AC-F002-002: 古代都市の形状と機能

Preconditions:
対応 TFC バイオームに Ancient City が生成される検証用ワールド、または Vanilla の Ancient City テンプレートを配置する試験環境がある。

Action:
代表的な建築区画、Sculk 装置、羊毛、隠し回路と Warden の出現・反応を、同一の Vanilla テンプレートと比較する。

Expected Result:
指定の 21 ブロック置換以外で構造物の配置・寸法・機能が変化しない。Sculk による感知、Shrieker、Warden の出現・AI・戦闘特性はバニラと同じである。

##### AC-F002-003: 追加自然生成の禁止

Preconditions:
本 MOD の worldgen registry と新規生成した Ancient City 周辺のチャンクがある。

Action:
本 MOD から登録した placed/configured feature と Loose Deepslate の自然配置を検査する。

Expected Result:
Loose Deepslate の地表散布、Hardened Deepslate の下面生成、独自 Deep Dark 地形のいずれも実装されない。

### F-003: Ancient City のブロック置換

#### Description

v0.1.1 の `TfcBlockReplacementProcessor` を基準に、23 件の固定置換から通常の焚き火と魂の焚き火の 2 件だけを削除し、**以下の 21 件だけ**を適用する。表にない Vanilla ブロック、全ての深層岩系ブロック、Sculk 系、魂の松明・魂のランタンは置換しない。TFC 岩石種の自動選択も実装しない。木材は常に Blackwood とする。

| 元の `minecraft:` ブロック ID | 置換先の完全な ID |
| --- | --- |
| `dark_oak_log` | `tfc:wood/log/blackwood` |
| `dark_oak_planks` | `tfc:wood/planks/blackwood` |
| `dark_oak_slab` | `tfc:wood/planks/blackwood_slab` |
| `dark_oak_stairs` | `tfc:wood/planks/blackwood_stairs` |
| `dark_oak_fence` | `tfc:wood/fence/blackwood` |
| `dark_oak_fence_gate` | `tfc:wood/fence_gate/blackwood` |
| `dark_oak_door` | `tfc:wood/door/blackwood` |
| `dark_oak_trapdoor` | `tfc:wood/trapdoor/blackwood` |
| `dark_oak_button` | `tfc:wood/button/blackwood` |
| `dark_oak_pressure_plate` | `tfc:wood/pressure_plate/blackwood` |
| `chest` | `tfc:wood/chest/blackwood` |
| `trapped_chest` | `tfc:wood/trapped_chest/blackwood` |
| `lectern` | `tfc:wood/lectern/blackwood` |
| `candle` | `tfc:candle` |
| `white_candle` | `tfc:candle/white` |
| `torch` | `tfc:torch` |
| `wall_torch` | `tfc:wall_torch` |
| `furnace` | `tfc:firepit` |
| `smoker` | `tfc:firepit` |
| `blast_furnace` | `tfc:firepit` |
| `iron_trapdoor` | `tfc:metal/trapdoor/wrought_iron` |

明示的な非置換対象は `minecraft:campfire`、`minecraft:soul_campfire`、`minecraft:deepslate`、`minecraft:cobbled_deepslate`、それらの階段・ハーフブロック・塀・レンガ・タイル・模様入り・ひび割れ系、`minecraft:reinforced_deepslate` である。焚き火は設置状態と機能をバニラのまま維持する。

適用範囲は `minecraft:ancient_city/*` の `StructureTemplate` のみとする。テンプレート ID を `StructureTemplateManager#getOrCreate` から伝搬し、`StructureTemplate#placeInWorld` で既存プロセッサの末尾へ 1 回だけ追加する v0.1.1 の仕組みを基本とする。変換先が存在しないときは元ブロックを保持して変換先 ID ごとに 1 回だけ警告する。

共通名・共通値を持つ `BlockState` property は引き継ぐ。Firepit についてはかまど類の水平向きから X/Z 軸を反映する。かまど・燻製器・溶鉱炉の元 block entity NBT は破棄する。それ以外の置換対象では、バニラの loot table 参照など必要な NBT を維持する。TFC Torch の消灯など、置換先 TFC ブロック固有の通常挙動は変更しない。

#### Acceptance Criteria

##### AC-F003-001: 21 種類の変換

Preconditions:
上表の元ブロックが各 1 個以上含まれる Ancient City テンプレート試験片が用意され、TFC の置換先 ID が登録されている。

Action:
テンプレートを配置し、各ブロックの登録 ID を検査する。

Expected Result:
表の 21 件が指定された ID へ変換され、予期しない 22 件目の置換はない。Blackwood 以外の木材は生成しない。

##### AC-F003-002: 非置換対象の保護

Preconditions:
Ancient City テンプレートに 2 種類の焚き火、深層岩系建材、Sculk、Redstone、羊毛、魂の照明が存在する。

Action:
対象テンプレートを配置し、各ブロックとその重要な機能を確認する。

Expected Result:
焚き火 2 種類は元のままで Firepit に変換されない。通常の深層岩と全ての派生建材、Sculk・回路・羊毛・魂の照明は元の ID と機能を維持する。

##### AC-F003-003: ブロック状態と NBT

Preconditions:
方向・開閉・段差・灯数などのプロパティが異なる元ブロックと、loot table を含む Chest NBT、元の NBT を持つ Furnace 類がある。

Action:
配置後の BlockState と BlockEntity を取得し、方向、互換プロパティ、Firepit 軸、NBT を比較する。

Expected Result:
置換先が保持する互換プロパティは維持される。Firepit は元の水平向きに対応する軸を持ち、不適合な Furnace 系 NBT は残らない。Chest の loot table など互換 NBT は保持される。

##### AC-F003-004: 適用範囲と欠落時の処理

Preconditions:
Ancient City 以外の Vanilla テンプレートと、意図的に欠落させた変換先を検査できる試験環境がある。

Action:
別構造物のテンプレート配置、同一 settings の再利用、欠落した変換先への処理を実行する。

Expected Result:
対象外構造物は変更されず、プロセッサの重複追加は起きない。変換先が存在しない場合は元ブロックを保持し、同じ変換先 ID への警告を重複出力しない。

### F-004: 古代都市の戦利品維持

#### Description

v0.1.1 の Ancient City チェストと Ice Box の戦利品、抽選回数、重み、数量、エンチャント処理、食料・鉱石・装備・バニラ固有報酬を **変更しない**。新規報酬を追加せず、鋼製の装備報酬を弱体化しない。以下の同梱データを固定参照の同名ファイルから継承し、JSON として有効な状態でロードする。

| 種別 | 継承する `src/main/resources/data/` 以下のファイル |
| --- | --- |
| チェスト | `minecraft/loot_table/chests/ancient_city.json` |
| 氷室 | `minecraft/loot_table/chests/ancient_city_ice_box.json` |
| 共有抽選 | `tfcancientcity/loot_table/shared/coal_like.json`、`gems.json`、`produce.json` |
| 鋼装備 | `tfcancientcity/loot_table/shared/equipment/lategame/greaves.json`、`hoe.json`、`horse_armor.json` |
| 食料の集合 | `tfcancientcity/tags/item/loot/produce.json` |

照合用の主要な定数は次のとおりとする。通常チェストは第一プールが 5～10 回、28 エントリ、第二プールが 1 回で、`empty:75`、`ward_armor_trim_smithing_template:4`、`silence_armor_trim_smithing_template:1` の重みを持つ。氷室は 4～10 回、5 エントリである。共有 `gems` は 9 種類、`produce` タグは 39 種類、`coal_like` は 4 種類である。詳細なエントリ・重み・関数は固定参照の JSON を正本とする。TFC 食料には TFC 本来の腐敗処理を適用する。

Vanilla 名前空間の戦利品テーブルを置き換える既存手段は維持するが、`minecraft:` の他の宝箱は変更しない。依存更新で JSON フォーマットの移行が必要なら、等価な抽選内容と確率を維持した上で検証する。

#### Acceptance Criteria

##### AC-F004-001: 戦利品データの完全性

Preconditions:
v0.1.1 固定参照の全 9 ファイルと v1.0 の同梱データがある。

Action:
JSON を解析して再帰的に比較し、テーブル参照先とアイテム ID のロードを検証する。

Expected Result:
フォーマット移行を要しないデータは意味的に一致する。移行が必要な場合もエントリ、重み、数量、抽選回数、付加効果、参照先の意味が一致し、未知・欠落 ID によるロードエラーがない。

##### AC-F004-002: Ancient City の抽選結果

Preconditions:
通常チェストと Ice Box の loot table が読み込まれ、複数の固定乱数シードを用意している。

Action:
各テーブルの結果を生成し、プール・エントリの分布、TFC の鋼装備・食料・宝石・石炭類、バニラ固有報酬を検査する。

Expected Result:
上記プール構成と v0.1.1 の抽選仕様が維持される。残響の欠片、レコード、鍛冶型、エンチャント本などの固有報酬が削除されず、TFC 装備と食料の参照が解決される。

##### AC-F004-003: 置換された Chest での loot 維持

Preconditions:
Ancient City 内に NBT で loot table を指定した Vanilla Chest と Trapped Chest の配置データがある。

Action:
Blackwood Chest への置換後、保存・再読込を経てチェストを開く。

Expected Result:
両 Chest の loot table 参照と抽選動作が保持され、内容の消失・空箱化・クライアントとサーバーの不一致が起きない。

### F-005: Loose Deepslate の登録・配置・表示

#### Description

既存の `tfcancientcity:rock/loose/deepslate` を通常の Item から **Block と BlockItem** に変更する。TFC Overdims v0.1.4 の `LooseRockBlock` と同じ `GroundcoverBlock` ベースを使用し、`count=1..4` の BlockState、段階別モデル、積み増し、素手回収、支持面判定、破壊時の個数回収を実装する。既存の `tfcancientcity:brick/deepslate` は引き続き独立した Item とする。

1 個を支持面上に設置でき、同じアイテムを右クリックすると 4 個まで 1 個ずつ追加される。5 個目は消費しない。TFC の地表小石と同じく、素手での回収時は設置数と同数を取得する。破壊時の loot table も `count` を反映する。支持面を失った場合は小石が落下・消失せず、対応するアイテム個数として回収される。

モデルは 1～4 個の見た目を持ち、石材・地表小石として認識できる深層岩のテクスチャを使用する。既存 v0.1.1 の `textures/item/loose_rock/deepslate.png` と `textures/item/brick/deepslate.png` は素材として再利用する。BlockState、ブロックモデル、アイテムモデル、適切なアイテム名を登録する。自然生成する placed feature は追加しない。

#### Acceptance Criteria

##### AC-F005-001: Loose Block と BlockItem

Preconditions:
TFC と本 MOD を導入し、`tfcancientcity:rock/loose/deepslate` を取得している。

Action:
Block / Item のレジストリと BlockItem の参照先を確認し、支持面の上にアイテムを設置する。

Expected Result:
既存 ID に Block と BlockItem がそれぞれ登録される。設置時に `count=1` の Loose Deepslate が生成され、手持ちアイテムが 1 個消費される。

##### AC-F005-002: 4 個までの積み増しと回収

Preconditions:
Loose Deepslate が 1 個設置され、残り 4 個以上の同一アイテムがある。

Action:
同じ位置へ繰り返し使用し、各 `count` の素手回収、通常破壊、支持面除去を調べる。

Expected Result:
`count` が 1、2、3、4 と増加し、5 個目は消費されない。回収・破壊・支持面除去後に、それぞれ表示数と同数の Loose Deepslate が得られる。アイテムの増殖・消失がない。

##### AC-F005-003: モデルと表示

Preconditions:
クライアントで `count=1..4` の Loose Deepslate と Deepslate Brick のアイテムを表示する。

Action:
各段階を E2E で撮影し、地面上のモデルとインベントリ表示を確認する。

Expected Result:
1～4 個の段階ごとに見た目が区別され、表示数と BlockState が一致する。モデル欠落、紫黒の欠落テクスチャ、アイテムの不可視化がない。

### F-006: 深層岩の採掘と孤立回収

#### Description

`minecraft:deepslate` と `minecraft:cobbled_deepslate` のブロック自体を置換せず、両者のブロック loot table を TFC Overdims の非孤立採掘方式へ揃える。通常の非孤立採掘では `tfcancientcity:rock/loose/deepslate` を一様乱数で **2～4 個**落とす。TFC の `tfc:is_isolated` 条件を満たす場合は破壊した元ブロックを **1 個**落とす。`tfc:breaks_when_isolated` には上記 2 種類の Vanilla ブロックを `replace: false` で追加し、TFC の既存孤立検出・回収機構を利用する。

この変更は古代都市内だけでなく、対象 Vanilla ブロックを採掘する全ての場所に適用する。v0.1.1 の通常ドロップ 1～4 個は 2～4 個へ変更する。シルクタッチなどに独自の特例を増やさず、TFC Overdims のブロック戦利品・孤立判定の順序を基準とする。深層岩鉱石、強化された深層岩、深層岩レンガ・タイル等の loot table は変更しない。

#### Acceptance Criteria

##### AC-F006-001: 非孤立状態の採掘

Preconditions:
他の岩石と接続された `minecraft:deepslate` と `minecraft:cobbled_deepslate` がある。

Action:
両者に対し複数の固定乱数シードで loot を生成し、サバイバルで実際に採掘する。

Expected Result:
両ブロックとも Loose Deepslate が 2～4 個得られ、1 個および 5 個以上の抽選は発生しない。実際の採掘でも同じ種類・個数のアイテムが得られる。

##### AC-F006-002: 孤立状態の元ブロック回収

Preconditions:
TFC の孤立 loot context と、周囲の接続を取り除いた各深層岩ブロックがある。

Action:
`tfc:is_isolated` の loot を生成し、TFC の既存 WorldTracker による孤立回収を実行する。

Expected Result:
`minecraft:deepslate` は元の Deepslate を 1 個、`minecraft:cobbled_deepslate` は元の Cobbled Deepslate を 1 個返す。両ブロックが `tfc:breaks_when_isolated` に属し、TFC の既存孤立回収が動作する。

##### AC-F006-003: 派生ブロックの採掘保護

Preconditions:
深層岩鉱石、強化された深層岩、階段、レンガ、タイルと、深層岩以外の一般石材がある。

Action:
各ブロックの loot table と実際の採掘結果を検査する。

Expected Result:
本 MOD による Loose Deepslate ドロップや孤立回収の変更は、指定の 2 種類以外に波及しない。

### F-007: Cobble Deepslate と石材加工

#### Description

TFC Overdims v0.1.4 の `cobble_end_stone` / `cobble_netherrack` に相当する独立したブロック `tfcancientcity:rock/cobble/deepslate` とその BlockItem を新規登録する。TFC の `Rock.BlockType.COBBLE` 相当の挙動・適切なモデル・テクスチャ・loot table を備える。**これは `minecraft:cobbled_deepslate` とは異なるブロック**であり、Ancient City の既存建材や Vanilla レジストリ ID を置換しない。新 Cobble Deepslate は通常破壊時に自身のアイテム 1 個を返す。

以下のレシピを実装する。工具を使うレシピは TFC の `advanced_shapeless_crafting` と `damage_crafting_remainder` を用い、工具を 1 回分消耗させる。レンガ・モルタル製作では材料を通常どおり消費する。

| 入力 | 出力 | 規則 |
| --- | --- | --- |
| Loose Deepslate ×4 | 新 Cobble Deepslate ×1 | 2×2 配置。旧「Loose ×4 → Vanilla Cobbled Deepslate」を置換する |
| 新 Cobble Deepslate ×1 | Loose Deepslate ×4 | 形状なしクラフト |
| `minecraft:cobbled_deepslate` ×1 | Loose Deepslate ×4 | v0.1.1 の既存逆変換を維持する |
| Loose Deepslate ×1 ＋ `#c:tools/chisel` | `tfcancientcity:brick/deepslate` ×1 | Chisel 耐久を 1 消費する |
| Deepslate Brick 素材 ×5 ＋ `tfc:mortar` ×4 | `minecraft:deepslate_bricks` ×4 | `BMB / MBM / BMB`、B は素材、M は Mortar |
| `minecraft:deepslate_bricks` ×1 ＋ `#c:tools/hammer` | `minecraft:cracked_deepslate_bricks` ×1 | Hammer 耐久を 1 消費する |

Loose Deepslate を `tfc:rock_knapping` と `tfc:stones/loose/metamorphic` の Item Tag に `replace: false` で追加し、TFC 本来の変成岩向け石器 Knapping へ接続する。Knapping GUI の素材画像も追加する。石器の新規 Tool Tier・Tool Head は作らない。

`tfc:item_size` により、Loose Deepslate と新 Cobble Deepslate は TFC 標準小石と同じ `small` / `light`、Deepslate Brick 素材は TFC 標準レンガ素材と同じ `very_small` / `very_light` とする。三者の定義が重複しないことを検査する。

Vanilla の深層岩系製作・石切台レシピを包括的に削除する変更は実施しない。TFC Overdims に含まれる別石材向けの Vanilla レシピ無効化処理は、本 MOD へ無条件に移植しない。v0.1.1 の独自 5 レシピについては、上表の 2×2 出力先変更と新 Cobble の逆変換追加以外の加工結果を維持する。

#### Acceptance Criteria

##### AC-F007-001: Cobble の登録と独立性

Preconditions:
新 Cobble Deepslate、Vanilla Cobbled Deepslate と TFC の石材ブロックがある。

Action:
ブロック・アイテム・モデル・破壊 loot の登録と、Ancient City への配置結果を検査する。

Expected Result:
`tfcancientcity:rock/cobble/deepslate` が Block と BlockItem として存在し、破壊時は自身を 1 個返す。Vanilla Cobbled Deepslate は別 ID のままであり、Ancient City の既存ブロックは変換されない。

##### AC-F007-002: 小石と Cobble の相互変換

Preconditions:
Loose Deepslate 4 個、新 Cobble Deepslate 1 個、Vanilla Cobbled Deepslate 1 個がある。

Action:
各入力から上表の 3 レシピを個別に製作し、旧 2×2 製作結果も確認する。

Expected Result:
Loose ×4 から新 Cobble ×1、新 Cobble ×1 から Loose ×4、Vanilla Cobbled ×1 から Loose ×4 を取得する。Loose ×4 のレシピは Vanilla Cobbled を出力しない。

##### AC-F007-003: レンガ加工と工具耐久

Preconditions:
Loose Deepslate、Chisel、Hammer、Mortar、Vanilla Deepslate Bricks がある。

Action:
上表の Chisel 加工、5 素材と 4 Mortar の交互配置、Hammer 加工を行い、材料・工具の残量を確認する。

Expected Result:
Deepslate Brick 素材 1 個、Vanilla Deepslate Bricks 4 個、Cracked Deepslate Bricks 1 個を各指定レシピで取得する。工具は消費されず耐久だけ 1 減り、Mortar と素材は指定数を消費する。

##### AC-F007-004: Knapping とサイズ・重量

Preconditions:
Loose Deepslate、新 Cobble Deepslate、Deepslate Brick 素材と、TFC の Knapping・Size/Weight 設定がある。

Action:
Loose Deepslate で変成岩の石器 Knapping を実行し、3 種類のサイズ・重量とタグ、GUI 表示を確認する。

Expected Result:
既存 TFC の変成岩向け石器頭部を製作でき、Knapping GUI に深層岩の素材が描画される。Loose と Cobble は `small` / `light`、Brick 素材は `very_small` / `very_light` に一致し、各サイズ定義は 1 件である。

### F-008: 崩落の禁止と Cobble の地滑り

#### Description

**Vanilla Deepslate とその派生物は TFC の崩落対象としない。** `minecraft:deepslate`、`minecraft:cobbled_deepslate` とその他深層岩建材を、本 MOD から `tfc:can_start_collapse`、`tfc:can_collapse`、`tfc:can_trigger_collapse` に追加しない。深層岩用の `tfc:collapse` recipe、崩落時に元岩を Cobble へ変える独自処理、構造物配置後の下面検査を実装しない。

**Hardened Deepslate は実装しない。** `tfcancientcity:rock/hardened/deepslate` の Block、Item、レシピ、モデル、テクスチャ、自然生成を追加せず、真下が Air / Cave Air の深層岩もそのまま生成する。

ただし、新規 `tfcancientcity:rock/cobble/deepslate` だけは、TFC Overdims と同様に `tfc:can_landslide` へ `replace: false` で追加し、TFC の既存地滑り処理を使用する。既存 `minecraft:cobbled_deepslate` は地滑り対象へ追加しない。F-006 の `tfc:breaks_when_isolated` は崩落とは別の仕組みとして維持する。

#### Acceptance Criteria

##### AC-F008-001: 深層岩の崩落と硬化の禁止

Preconditions:
Vanilla Deepslate、Vanilla Cobbled Deepslate、他の深層岩建材で作られた支持なし構造と、Air / Cave Air に下面が接する Ancient City テンプレートがある。

Action:
TFC 崩落タグ・崩落 recipe・登録 Block・配置後の構造物を検査し、崩落判定を実行する。

Expected Result:
本 MOD 由来の崩落タグ・recipe がなく、接続状態を保持した深層岩に崩落は発生しない。`rock/hardened/deepslate` は存在せず、下面が空気の深層岩も Hardened へ置換されない。孤立回収試験は F-006 に従って別途成立する。

##### AC-F008-002: 新 Cobble の地滑りと他ブロックの保護

Preconditions:
新 Cobble Deepslate と Vanilla Cobbled Deepslate を、TFC の地滑り判定を行える位置へ配置している。

Action:
両者のタグを検査し、支持面を変えて地滑り処理を実行する。

Expected Result:
新 Cobble Deepslate のみが本 MOD の `tfc:can_landslide` 追加対象であり、TFC の地滑りを起こす。Vanilla Cobbled Deepslate、Deepslate Bricks、Tiles、鉱石は本 MOD によって地滑り対象にならない。

## Global Requirements

1. **仕様とソースの照合**：実装開始前に固定参照の TFC Ancient City v0.1.1 と TFC Overdims v0.1.4 の Java・JSON・登録 ID を確認する。依存更新時は参照元コードを無条件にコピーせず、利用中の TFC・NeoForge の実 API / レジストリ / JAR データと比較する。既存 `AGENTS.md` が本書と矛盾する場合は本書を優先し、運用文書の旧仕様も整合させる。
2. **変更範囲**：変更対象は本 MOD のリポジトリに限定する。Vanilla や TFC のソース・既存地形を直接書き換えず、必要な互換データだけを `minecraft:`・`tfc:` の名前空間へ追加・上書きする。Sculk、Warden、Ancient City の生成パラメータを変更しない。
3. **ID とデータ**：既存 3 ID を維持し、新規 Cobble は `tfcancientcity:rock/cobble/deepslate` とする。JSON は BOM のない UTF-8 とし、全参照を registry と照合する。新旧の Vanilla Deep Slate 系ブロック ID を独自ブロックで再登録しない。
4. **実装品質**：関連コードの設計は必要な処理範囲に限定する。BlockState・BlockEntity NBT・サーバーとクライアントの同期・保存と再読込を保護する。未知 ID やデータロード失敗を黙殺せず、明示的な例外・警告で診断する。未実装機能を成功と見なす空のテストや、常に成功するアサーションを作らない。
5. **ハーネスでの検証**：全 AC を `tests/acceptance.json` の GameTest または E2E に少なくとも 1 件対応させる。ゲームロジック、JSON・レジストリ、レシピ、loot、崩落の判定は GameTest を基本とする。表示・モデルは E2E の撮影結果で検証する。GameTest の JUnit XML と testcase 名を正確に対応させる。
6. **回帰検証**：少なくとも 21 対応表の全入力、2 種類の焚き火、Vanilla 深層岩の建材、33 バイオーム、通常・氷室 loot、孤立と非孤立の採掘、1～4 個の Loose 表示、加工工具の耐久、新 Cobble の地滑り、Sculk・Warden を検証する。単一の代表入力の合格だけで他の入力を合格扱いにしない。
7. **成果物**：ソース、リソース、GameTest、E2E シナリオ、`tests/acceptance.json`、必要な build / runtime 設定を実装対象とする。`./gradlew classes`、`./gradlew build`、`./gradlew runGameTestServer` および適用する E2E の成功を完了条件とする。ハーネスが実際に使う Gradle task と JUnit report の出力先を一致させる。
8. **配布データ**：既存 `en_us` の翻訳を維持し、新規 Block / Item の欠落名をなくす。リポジトリの `LICENCE` と配布 metadata のライセンス表記を一致させる。リリース成果物に開発用の外部 JAR、解析ファイル、検証用ワールドを含めない。

## Constraints

- Minecraft は **1.21.1 のみ**。NeoForge・TFC の Minecraft 1.21.1 向け更新への対応は許容するが、Minecraft 1.20.1・1.21.4 等の別版対応は本件に含めない。
- Ancient City の形状・生成条件は v0.1.1 / Vanilla 1.21.1 のまま維持する。現行の 33 バイオーム定義を変更しない。
- 固定ブロック置換は表の 21 件に限定する。`campfire` と `soul_campfire` を Firepit へ変換しない。Furnace、Smoker、Blast Furnace は Firepit へ置換する。
- 深層岩、深層岩建材、Sculk、Warden を独自の生成・崩落・戦闘規則で再実装しない。Hardened Deepslate、下面硬化、Loose Deepslate の自然生成を追加しない。
- 独自の設定ファイル・設定画面・設定コマンド・ユーザー向け切替機能を作らない。実装に必要な JSON データと TFC タグは設定機能に含めない。
- 戦利品は v0.1.1 の確率・量・種類を維持する。旧ワールド・旧インベントリからの移行、既存セーブデータの互換性は保証しない。ただし既存 ID を変更する必要はない。
- TFC Overdims の Nether / End、気候、木材、MOB、Hardened、地質全般などの機能は移植しない。深層岩に必要な Loose、Cobble、採掘、孤立、Knapping、サイズ・重量、Cobble の地滑りに限定して参照する。
- 他 MOD との完全な互換性は保証しないが、本 MOD が無関係な構造物やブロックの処理へ介入しないようにする。
- 本書は modding-harness の `PROJECT.md` に対応する製品仕様の正本である。実装工程で仕様を独断変更せず、必要なら仕様変更を独立した作業として扱う。

## Open Questions

None.
