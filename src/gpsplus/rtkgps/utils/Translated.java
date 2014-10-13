package gpsplus.rtkgps.utils;

public enum Translated {
        de("ger"),
        de_DE("deu"),
        en("eng"),
        fr("fre"),
        fr_FR("fra"),
        es("spa"),
        pl("pol"),
        cn("chi"),
        zh_rCN("zho");

        private final String iso639_2;

        Translated(String locale) {
            this.iso639_2 = locale;
        }

        public static boolean contains(String test) {

            for (Translated t : Translated.values()) {
                if (t.name().equals(test) || t.iso639_2.equals(test)) {
                    return true;
                }
            }

            return false;
        }
}
