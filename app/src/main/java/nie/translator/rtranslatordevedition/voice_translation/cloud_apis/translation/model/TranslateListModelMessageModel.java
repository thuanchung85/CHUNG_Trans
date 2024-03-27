package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.model;


public class TranslateListModelMessageModel {
    TranslateModel[] translations;

    public TranslateListModelMessageModel(TranslateModel[] translations) {
        this.translations = translations;
    }

    public TranslateModel[] getTranslations() {
        return translations;
    }

    public void setTranslations(TranslateModel[] translations) {
        this.translations = translations;
    }
}
