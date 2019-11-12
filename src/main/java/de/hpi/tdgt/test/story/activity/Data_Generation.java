package de.hpi.tdgt.test.story.activity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Data_Generation extends Activity {
    private String[] data;
    private String table;

    @Override
    public void perform() {

    }
}
