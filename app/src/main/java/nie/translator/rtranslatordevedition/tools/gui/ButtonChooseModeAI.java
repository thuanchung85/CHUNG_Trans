/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.tools.gui;

import android.content.Context;
import android.util.AttributeSet;
import nie.translator.rtranslatordevedition.R;


public class ButtonChooseModeAI extends DeactivableButton {
    private int aimode=0;

    public ButtonChooseModeAI(Context context) {
        super(context);
    }

    public ButtonChooseModeAI(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonChooseModeAI(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getAimode() {
        return aimode;
    }

    public void setAimode(int choose_aimode){
        aimode = choose_aimode;
        switch (aimode){
            case 0:
                setImageDrawable(getResources().getDrawable(R.drawable.sendmsg,null));
                break;
            case 1:
                setImageDrawable(getResources().getDrawable(R.drawable.openai_icon,null));
                break;

                default: setImageDrawable(getResources().getDrawable(R.drawable.google_cloud,null));
        }

    }

}
