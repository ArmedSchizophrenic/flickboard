# FlickBoard 0.3.0 — podpowiedzi PL / EN

- Pasek maksymalnie trzech podpowiedzi nad klawiaturą.
- Uzupełnianie i ręczny wybór poprawionej pisowni, np. `gura` → `góra`.
- Po spacji: przewidywanie kolejnego słowa na podstawie lokalnej historii pisania.
- Polski i angielski: oddzielne słowniki offline i oddzielne historie uczenia.
- Settings → Behaviour: wybór języka, przełączniki podpowiedzi i uczenia oraz
  usuwanie historii obu języków.
- Brak uczenia w hasłach; respektowanie prośby edytora o wyłączenie personalizacji.
- Historia pozostaje w prywatnym katalogu aplikacji wyłączonym z backupu Androida.

Polski jest domyślnym językiem podpowiedzi. Język angielski można wybrać w
Settings → Behaviour → Suggestion language. Uczenie jest domyślnie włączone;
trafność przewidywania kolejnych słów rośnie wraz z historią pisania.
Wybór podpowiedzi jest ręczny. Brak automatycznego rozpoznawania języka.

Android 9 lub nowszy. APK jest podpisane lokalnym kluczem tego forka. Jeśli
masz już aplikację o tym samym identyfikatorze podpisaną innym kluczem,
Android nie pozwoli zainstalować aktualizacji. Przed ewentualnym odinstalowaniem
poprzedniej aplikacji wyeksportuj jej ustawienia.

Słowniki: Hermit Dave / FrequencyWords / OpenSubtitles2018, CC BY-SA 4.0,
z normalizacją i filtrowaniem opisanymi w `app/src/main/assets/SUGGESTIONS-LICENSE.md`.

Walidacja: 16 testów podpowiedzi (silnik, edycja tekstu, pola wrażliwe),
3 testy tekstowe oraz lint wersji release przechodzą. Testy integracyjne Androida
kompilują się; nie zostały uruchomione, ponieważ lokalny emulator ulega awarii
podczas startu. Działanie na fizycznym urządzeniu wymaga jeszcze sprawdzenia. Istniejący test
`SymbolLayerTest` ujawnia niezwiązane z tą zmianą nakładanie symboli w układzie
arabskim. Nie został zmieniony układ klawiszy arabskich.
