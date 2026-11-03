# FlickBoard 0.4.0 — słownik osobisty i podpowiedzi PL / EN

- Podczas pisania pierwsza podpowiedź to bieżące słowo dokładnie w wpisanej
  postaci. Kliknięcie zatwierdza je, dodaje spację i zapisuje w osobistym słowniku
  aktywnego języka. Pozostałe dwa miejsca służą do uzupełnień i poprawek.
- Po spacji wszystkie trzy miejsca proponują kolejne słowa.
- Settings → Personal dictionary: osobne listy ręcznie zapisanych słów PL / EN
  oraz usuwanie pojedynczych wpisów wraz z ich wyuczonymi powiązaniami.
- Ręczny zapis działa także przy wyłączonym automatycznym uczeniu. Pola haseł
  i edytory żądające wyłączenia personalizacji nie zapisują słów.
- Język podpowiedzi automatycznie podąża za aktywnym Letter layout. W Behaviour
  można wyłączyć automat i ustawić język ręczny / zapasowy.
- Podpowiedzi obsługują tekstowe pola wyszukiwania, paski adresu oraz edytory
  udostępniające niepełny tekst wokół kursora. Dotyczy to typów pól używanych
  m.in. do wyszukiwania w przeglądarkach i aplikacjach.
- Domyślny Key height: 64 dp. Zapisane indywidualne ustawienia są zachowane.
- Settings → Layout → Suggestion bar height: 32–80 dp, domyślnie 48 dp.
- Domyślne tło klawiatury: dołączone cat.jpg. Własny obrazek wybrany
  w ustawieniach nadal ma pierwszeństwo.

Wszystko działa lokalnie. Dane PL i EN pozostają oddzielne, a zapisane słowa
przetrwają ponowne uruchomienie. Usunięcie wpisu osobistego nie usuwa słowa
ze słownika wbudowanego, jeżeli już w nim występuje.

APK jest podpisane tym samym kluczem co v0.3.0 i v0.3.1. Instalacja jako
aktualizacja zachowuje ustawienia oraz dotychczasową historię uczenia.

Walidacja: 38 testów silnika, zapisu i odczytu słownika z dysku, ręcznego zapisu
przy wyłączonym uczeniu, rozdzielenia języków, usuwania w ekranie ustawień,
przełączania układów, pól wyszukiwania, zmiany rozmiaru i edycji tekstu oraz lint.
Sprawdzenie konkretnych aplikacji na fizycznym telefonie pozostaje do potwierdzenia.
W pełnym zestawie pozostaje wcześniejszy błąd SymbolLayerTest dotyczący układu
arabskiego; nie dotyczy on podpowiedzi ani słownika osobistego.
