# FlickBoard 0.3.1 — poprawka zmiany rozmiaru klawiatury

- Po zmianie rozmiaru klawiatury Android ponownie przelicza obszar dotyku,
  także gdy okno klawiatury nadal zajmuje ten sam obszar ekranu.
- Klawisze kierują kliknięcia i gesty do bieżącej sesji wpisywania po powrocie
  z ustawień lub zmianie pola tekstowego.
- Nowa sesja otrzymuje aktualny stan klawiszy modyfikujących bez dodatkowego
  naciśnięcia.

APK jest podpisane tym samym kluczem co v0.3.0, więc można je zainstalować
jako aktualizację, zachowując ustawienia i wyuczone słowa.

Walidacja: trzy testy regresji Android/Compose uruchamiane na komputerze
przez Robolectric: zmiana rozmiaru i sesji (kliknięcia oraz gesty), stan
modyfikatorów i przeliczanie obszaru dotyku przy powiększaniu oraz pomniejszaniu.
Wszystkie trzy wykrywają stare zachowanie i przechodzą z poprawką. Dodatkowo
przechodzi 16 testów podpowiedzi, 3 testy tekstowe i lint wersji release.
Poprawka wymaga jeszcze potwierdzenia na fizycznym urządzeniu.

W pełnym zestawie testów nadal występuje wcześniejszy, niezwiązany błąd
`SymbolLayerTest` dotyczący nakładania symboli w układzie arabskim.
