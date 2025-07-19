# 🛠️ Lignes directrices pour contribuer

*Les pull requests, signalements de bugs et toutes autres formes de contribution sont les bienvenues et encouragées !*

> **Ce guide a pour but de donner des normes pour ce projet afin de rendre le code plus lisible, maintenable et simple à faire évoluer.**  
> Ce projet est une **librairie graphique pour Minecraft (Forge/Fabric)**. Il est donc essentiel que le code soit **propre, documenté** et respecte des **conventions strictes** pour garantir la compatibilité avec les futurs mods.

---

## 📖 Code de conduite
Merci de lire notre [Code de conduite](CODE_OF_CONDUCT.md) avant toute contribution.  
Soyons respectueux et constructifs dans nos échanges.

---

## 📦 Installation et environnement de développement
Avant de contribuer, assurez-vous d'avoir :
- **Minecraft Fabric** version correspondante installée.
- **Java 21 ou plus récent**.
- **Gradle** correctement configuré (`gradlew build` doit fonctionner).
- Vérifié que le projet compile et s’exécute avant vos modifications.

Pour tester vos changements :
1. Placez la librairie compilée dans un dossier `mods/` d’une instance Minecraft dédiée au test.
2. Vérifiez que vos ajouts **n’introduisent pas de crashs ni de régressions visuelles** (ex. pas de HUD cassé ni de shader qui clignote).

---

## 📥 Ouvrir une Issue
Avant d’ouvrir une issue :
- **Cherchez un doublon**. Si un problème existe déjà, utilisez un 👍 ou ajoutez des informations utiles.
- Assurez-vous d’avoir testé sur la **dernière version du mod**.
- Remplissez le **template d’issue complet**.

### 🪲 Rapport de bug
Un bon rapport de bug contient :
- Version exacte de Minecraft et de Forge/Fabric.
- Capture d’écran ou vidéo si le bug est graphique (ex. shader qui rend mal le flou).
- Log complet si un crash survient.
- Étapes pour reproduire le bug.

Exemple :
```
Version: Minecraft 1.20.1 - Forge 47.2.0
Bug: Le HUD transparent disparaît après un redimensionnement d’écran.
Étapes :

Ouvrir un menu utilisant la librairie.

Redimensionner la fenêtre Minecraft.

Le HUD devient invisible jusqu'à relog.

Log: https://pastebin.com/xxxx
```


---

## 🔁 Travailler sur une Pull Request (PR)

### 1. Fork & branche
- **Forkez** le projet et clonez votre fork.
- **Ne travaillez jamais sur la branche `main` !** Créez une branche spécifique :
```bash  
git checkout -b feat/add-rounded-button
```
- **Nommez vos branches clairement** :  
  `feat/add-[nom]`, `fix/fix-[nom]`, `docs/update-readme`.

Plus d'information ici -> [Comment nommer ses branches et ses commits ?](https://www.codeheroes.fr/2020/06/29/git-comment-nommer-ses-branches-et-ses-commits/)

### 2. Règles générales
✅ **À faire** :
- Une PR = **un changement unique** (ex. ajout d’un shader OU correction d’un widget, pas les deux).
- Testez toujours vos changements en jeu avant de soumettre.
- Ajoutez des **commentaires Javadoc** sur toutes nouvelles méthodes publiques.
- Respectez les **conventions du style de code** (voir plus bas).

❌ **À ne pas faire** :
- Ne reformatez pas tout un fichier pour corriger une ligne.
- Ne modifiez pas des parties sans rapport avec votre PR.
- N’envoyez pas de PR partiellement terminée.

### 3. Exemples de PR acceptables
- ✅ **Ajout d’un nouveau composant graphique** :  
  *"Ajoute un bouton arrondi avec animation de survol"*  
  → Fichier modifié : `RoundedButtonWidget.java`.

- ✅ **Correction d’un shader** :  
  *"Corrige l’artefact visuel du flou sur les HUD transparents"*  
  → Fichiers modifiés : `blur.frag`, `BlurProgram.java`.

- ✅ **Amélioration de performance** :  
  *"Optimise le rendu des bordures 1px des fenêtres"*.

---

## 📝 Écrire un bon message de commit
Suivez [ce guide](https://cbea.ms/git-commit/) et utilisez ce format :  
```
[TAG] Sujet du commit (max 50 caractères)

Description complète (wrap à 72 caractères). Expliquez ce qui a été
modifié et pourquoi. Ajoutez des références aux issues si possible.

Résout: #123
Voir aussi: #456
```

Exemples :
```
[FEAT] Ajoute un widget de slider arrondi

Ce widget permet aux développeurs de mods de créer des sliders avec des
bords arrondis et des animations fluides. Testé sur Forge 1.20.1.

Résout: #34
```

```
[BUG] Corrige le clignotement du shader de flou

Le shader BLUR causait un clignotement lors des changements rapides de
résolution. La mise à jour synchronise correctement le RenderTarget.
```

---

## ✅ Revue de code
- **Examinez le code, pas l’auteur**.
- Donnez des critiques constructives, proposez des alternatives si besoin.
- Avant d’approuver, assurez-vous que :
    - Le code compile sans erreur (`gradlew build`).
    - Les nouveaux ajouts fonctionnent en jeu.

---

## 💅 Style de code
Respectez les conventions officielles Java ([Oracle Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html)).

| Élément | Règle | Exemple |
|---------|-------|---------|
| **Packages** | minuscules, pas d’underscore | `com.mylib.render.blur` |
| **Classes** | `PascalCase` | `BlurProgram`, `RoundedButtonWidget` |
| **Méthodes** | `camelCase`, commence par un verbe | `renderBlur()`, `initShader()` |
| **Variables** | `camelCase`, noms explicites | `int widgetWidth`, `ShaderInstance blurShader` |
| **Constantes** | `UPPER_SNAKE_CASE` | `static final int MAX_RADIUS = 10;` |

---

## 🧪 Tests et compatibilité
- Testez **sur différentes résolutions** et **interfaces Minecraft** (GUI scale 2, 3…).
- Vérifiez la compatibilité avec d’autres mods graphiques (Optifine, Iris, etc.) si possible.
- Ajoutez des logs clairs uniquement en mode debug.

---

## 📄 Licence
Toutes les contributions sont sous licence **GPL-3.0**, comme le reste du projet.
