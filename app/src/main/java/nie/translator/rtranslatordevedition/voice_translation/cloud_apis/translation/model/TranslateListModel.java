package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.model;

import java.util.Arrays;

public class TranslateListModel {
    TranslateModel[] translations;
    DetectedLanguage detectedLanguage;

    public TranslateListModel(TranslateModel[] translations, DetectedLanguage detectedLanguage) {
        this.translations = translations;
        this.detectedLanguage = detectedLanguage;
    }

    public TranslateModel[] getTranslations() {
        return translations;
    }

    public void setTranslations(TranslateModel[] translations) {
        this.translations = translations;
    }

    public DetectedLanguage getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(DetectedLanguage detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }
}
