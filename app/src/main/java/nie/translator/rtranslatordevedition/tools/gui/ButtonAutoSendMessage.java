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


public class ButtonAutoSendMessage extends DeactivableButton {
    private boolean isEnable=false;

    public ButtonAutoSendMessage(Context context) {
        super(context);
    }

    public ButtonAutoSendMessage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonAutoSendMessage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(){
        isEnable = true;
        setImageDrawable(getResources().getDrawable(R.drawable.sendmsg,null));
    }
    public void setDisable(){
        isEnable = false;
        setImageDrawable(getResources().getDrawable(R.drawable.sendmsgdis,null));
    }
}
