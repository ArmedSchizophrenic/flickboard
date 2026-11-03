# FlickBoard 0.4.1 — zmiana języka podpowiedzi gestem

- Swipe w dół na spacji przełącza język podpowiedzi PL ↔ EN.
- Etykieta na spacji pokazuje aktualny język: PL lub EN.
- Zmiana języka nie wyświetla powiadomienia ani komunikatu nad klawiaturą.
- Zwykłe kliknięcie nadal wpisuje spację. Układ liter pozostaje bez zmian.
- Gest wybiera ręczny tryb języka podpowiedzi. Automatyczne dopasowanie do
  Letter layout można ponownie włączyć w Settings → Behaviour.

APK jest podpisane tym samym kluczem co wcześniejsze wydania. Można je
zainstalować jako aktualizację, zachowując ustawienia i osobiste słowniki.

Walidacja: 39 testów, w tym test rzeczywistego gestu na komponencie klawiatury, etykiet PL / EN,
braku komunikatu, wpisywania spacji oraz testy istniejących funkcji i lint.
Testy Android / Compose działają lokalnie przez Robolectric; potwierdzenie
na fizycznym telefonie pozostaje do sprawdzenia.
W pełnym zestawie testów pozostaje wcześniejszy błąd SymbolLayerTest
w układzie arabskim, niezwiązany z tą zmianą.
