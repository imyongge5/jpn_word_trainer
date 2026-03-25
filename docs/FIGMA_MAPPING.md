# Figma Mapping

## Figma file

- Name: `단어장 앱 UI`
- URL: `https://www.figma.com/design/XNcUCPgnIv2wLvFG1HtC4M/%EB%8B%A8%EC%96%B4%EC%9E%A5-%EC%95%B1-UI?t=63R42XsdSANQAEzj-0`
- File key: `XNcUCPgnIv2wLvFG1HtC4M`

## Screen mapping

- Figma frame `홈` (`3:2`)
  - Code: `HomeRoute`
  - File: `app/src/main/java/com/example/wordbookapp/ui/WordbookApp.kt`
- Figma frame `단어장 목록` (`3:33`)
  - Code: `DeckRoute`
  - File: `app/src/main/java/com/example/wordbookapp/ui/WordbookApp.kt`
- Figma frame `모든 단어` (`3:76`)
  - Code: `AllWordsRoute`
  - File: `app/src/main/java/com/example/wordbookapp/ui/WordbookApp.kt`
- Figma frame `단어사전` (`3:114`)
  - Code: `WordDetailRoute`
  - File: `app/src/main/java/com/example/wordbookapp/ui/WordbookApp.kt`

## Notes

- The current Figma plan is a `View` seat on a `starter` team, so Figma Code Connect mappings could not be saved through the MCP API.
- This file is the local source of truth for screen-to-code mapping until the Figma plan allows Code Connect.
- `scripts/build-test.ps1` opens the mapped Figma file after a successful build by default.
