package ch.mitto.missito.ui.signin.Picker;

import ch.mitto.missito.ui.common.fastscroll.RecyclerViewFastScroller;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;


public class CountryListAdapter extends SectionedRecyclerViewAdapter implements RecyclerViewFastScroller.BubbleTextGetter {
    @Override
    public String getTextToShowInBubble(int pos) {
        Section section = getSectionForPosition(pos);
        CountrySection country = ((CountrySection) section);
        return country.getFirstLetter();
    }
}
