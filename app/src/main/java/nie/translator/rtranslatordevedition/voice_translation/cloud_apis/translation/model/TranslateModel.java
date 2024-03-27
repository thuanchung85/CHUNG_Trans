package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.model;

public class TranslateModel {
    public String text ;
    public String to ;

    public TranslateModel(String text, String to) {
        this.text = text;
        this.to = to;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "TranslateModel{" +
                "text='" + text + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}



