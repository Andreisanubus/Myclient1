# MyClient (Fabric 1.21.11)

## Pornire
1. JDK 21 + IntelliJ IDEA.
2. Genereaza wrapper-ul Gradle (o data): `gradle wrapper --gradle-version 9.2.0`
3. `./gradlew genSources` apoi `./gradlew runClient`.
4. In joc: **Right Shift** deschide GUI-ul (se poate schimba din Controls).

## Controale in GUI
- Click stanga pe modul: porneste / opreste. Click dreapta: deschide setarile lui.
- Setari: comutator ON/OFF, slider (se trage), mod (stanga = urmatorul, dreapta = precedentul).
- `Bind`: click stanga, apoi apesi o tasta (Backspace/Delete sterge). Click dreapta pe Bind sterge.
- `Copy Config` / `Paste Config`: doar setarile acelui modul.
- Rotita mouse: deruleaza panourile. Tabul Config: profile, share code.

## Module cu logica scrisa (23)
Better Sus Chunk, Crystal Optimizer, Friend Guard, Auto Totem, Glow ESP, Trajectories, Low Health Warning,
Block ESP, Storage ESP, Nametags, Tracers, Waypoints, Xray (marcare minereuri, nu xray real), Zoom,
No Jump Delay, Sprint, Fly, Speed, Auto Walk, Auto Jump, Target ESP, Target HUD, Armor HUD, HUD Logo.

Restul (marcate cu * in GUI) sunt inregistrate cu setarile lor, dar nu au logica inca; la pornire arata un mesaj.

## ESP / randare
ESP-urile sunt proiectate manual si desenate pe HUD (clasa `render/Proj`), deci se vad prin pereti si prin pamant
fara sa depinda de API-ul de randare 3D al Minecraft. Pentru fluiditate maxima se poate trece mai tarziu la randare 3D.

## Modul nou
Scrie o clasa care extinde `Module`, ia setari cu `bool(..)`, `num(..)`, `mode(..)`, foloseste `onTick()` / `onRender(...)`,
apoi adaug-o in `Catalog.registerAll`. O setare apare doar cand vrei: `setare.visible = () -> altaSetare.value;`

## Build pe GitHub ddintr-un singur zip
Urci `myclient.zip` in radacina repo-ului. Workflow-ul din `.github/workflows/build.yml` il despacheteaza si compileaza,
deci nu mai conteaza structura de foldere din repo. Jar-ul apare la Actions > Artifacts.
