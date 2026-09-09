# Voice: zehn Filmöffnungen in einer Sitzung

9. September 2026, lokaler Test mit PydanticAI 2.42.0.

## Ergebnis

**10 von 10 verschiedenen Filmen wurden in derselben WebSocket-Sitzung geöffnet.** Geprüft wurden sowohl die Film-ID in der Route als auch der tatsächlich gerenderte Filmtitel. Englische Sprachbefehle wurden mit der lokalen macOS-Stimme Samantha synthetisch erzeugt und über einen virtuellen Mikrofonstream durch die echte Browser-Audioverarbeitung eingespeist. Python verwendete xAI und die echten Java-MCP-Tools. Kein reales Nutzermikrofon wurde aufgezeichnet.

| Nr. | Film | Katalog-ID | Angezeigter Titel | Ergebnis |
|---|---|---|---|---|
| 1 | Forrest Gump | 6 | Forrest Gump | geöffnet |
| 2 | The Matrix | 8 | The Matrix | geöffnet |
| 3 | Inception | 3 | Inception | geöffnet |
| 4 | Interstellar | 5 | Interstellar | geöffnet |
| 5 | Mad Max Fury Road | 59 | Mad Max: Fury Road | geöffnet |
| 6 | Fight Club | 4 | Fight Club | geöffnet |
| 7 | Pulp Fiction | 7 | Pulp Fiction | geöffnet |
| 8 | The Dark Knight | 2 | The Dark Knight | geöffnet |
| 9 | Gladiator | 16 | Gladiator | geöffnet |
| 10 | Titanic | 36 | Titanic | geöffnet |

## Reproduzierte Fehler

- **Vor der Korrektur:** acht Öffnungen erfolgreich; beim neunten Befehl `UsageLimitExceeded`, weil die nächste Modellrunde das Sitzungslimit von 16 überschritten hätte. Eine Suche samt sprachlicher Bestätigung belegte hier zwei Modellrunden. Zusätzliche Tool-Aufrufe können die Grenze früher erreichen.
- **Zwei Zwischenläufe:** `ConnectError` während automatischer lokaler Spring-Boot-Neustarts. Java meldete entsprechende Classpath-Änderungen. Das ist ein separater Fehlerpfad gegenüber dem Modellbudget.
- **Mad Max: Fury Road:** als fünfter Film mehrfach korrekt geöffnet. Die vorher vom Nutzer beobachtete ausbleibende Navigation wurde damit nicht reproduziert. Für dessen alte Abbrüche um 14:41:18 und 14:45:13 fehlen die damaligen Fehlerklassen; eine eindeutige rückwirkende Zuordnung ist nicht möglich.

## Korrektur

- Sitzungsbudget auf 32 Modellrunden und 24 Tools angehoben. Gesamtdauer und Tokenlimits bleiben begrenzt.
- SDK-Budgetüberschreitung wird an der Adaptergrenze in `VoiceSessionLimitError` übersetzt. Die UI erhält eine verständliche Budgetmeldung statt eines allgemeinen Verbindungsfehlers.
- Die Log-Allowlist behält jetzt die Fehlerklasse `error_type`. Rohe Exception-Messages und Gesprächsinhalte bleiben ausgeschlossen.
- Deterministische Regressionstests für zehn aufeinanderfolgende Öffnungen, Erreichen des weiterhin geltenden Budgets und die sichere WebSocket-Fehlermeldung ergänzt.

Der laufende lokale Java-Prozess wurde für stabile Sprachtests mit `JAVA_TOOL_OPTIONS=-Dspring.devtools.restart.enabled=false ./gradlew bootRun` gestartet. Dies ist ausschließlich eine Einstellung dieses Prozesses, keine Änderung an Backend- oder Kubernetes-Konfiguration. Ein normaler späterer Start aktiviert das übliche DevTools-Verhalten wieder. Der normale Python-Voice-Service auf Port 8090 wurde mit der Korrektur neu gestartet; Health antwortet erfolgreich.

## Verifikation

- `make verify-agent`: **142 Tests, 27 deterministische Evals**, Format, Lint, strikte Typprüfung und Architekturverträge erfolgreich.
- Echter Chromium-/xAI-/Java-Lauf: **10 Öffnungen, 10 passende gerenderte Titel, eine Voice-Verbindung**, keine Fehler.
- `git diff --check`: erfolgreich.
- Keine vollständige Java-/Frontend-Testwiederholung oder erneuter Container-Build: geändert wurden Python-Gesprächslogik und Logging, keine Abhängigkeiten, Container- oder Backendverträge.

Lokale Reproduktionsartefakte: [Browserablauf](/tmp/imdb-ten-voice-browser.mjs), [Vorher](/tmp/imdb-ten-voice-before.jsonl), [erfolgreicher Nachher-Lauf](/tmp/imdb-ten-voice-final.jsonl), [Providerdiagnose](/tmp/imdb-ten-voice-provider-final.log). Die `/tmp`-Dateien sind vergänglich; Ergebnis und Regressionstests bleiben im Repository erhalten.

Dieser Test deckt synthetische, nacheinander ausgesprochene Befehle ab. Unterbrechungen, akustisches Echo, mehrdeutige Filmtitel und Wiederaufnahme während eines Backend-Ausfalls bleiben gesonderte Test- und Verbesserungsfälle.
