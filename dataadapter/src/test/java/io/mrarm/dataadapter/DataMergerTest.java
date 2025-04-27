package io.mrarm.dataadapter;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DataMergerTest {

    private static ViewHolderType<String> type1;
    private static ViewHolderType<String> type2;

    @BeforeClass
    public static void initTypes() {
        type1 = ViewHolderType.from((ctx, parent) -> { throw new UnsupportedOperationException(); });
        type2 = ViewHolderType.from((ctx, parent) -> { throw new UnsupportedOperationException(); });
    }

    @Test
    public void testMergeItemList() {
        DataMerger merger = new DataMerger();
        merger.add(new SingleItemData<>("Header", type1));
        List<String> test = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            test.add("List" + i);
        merger.add(new ListData<>(test, type2));
        merger.add(new SingleItemData<>("Footer", type1));
        assertEquals(0, (int) merger.getStartIndexes().get(0));
        assertEquals(1, (int) merger.getStartIndexes().get(1));
        assertEquals(51, (int) merger.getStartIndexes().get(2));

        assertEquals(0, merger.getFragmentAt(0));
        assertEquals(1, merger.getFragmentAt(1));
        assertEquals(1, merger.getFragmentAt(50));
        assertEquals(2, merger.getFragmentAt(51));

        assertEquals(50 + 2, merger.getItemCount());
        assertEquals("Header", merger.getItem(0));
        assertEquals("Footer", merger.getItem(51));
        for (int i = 0; i < 50; i++)
            assertEquals(test.get(i), merger.getItem(i + 1));

        assertEquals(type1, merger.getHolderTypeFor(0));
        for (int i = 0; i < 50; i++)
            assertEquals(type2, merger.getHolderTypeFor(i + 1));
        assertEquals(type1, merger.getHolderTypeFor(51));
    }

    @Test
    public void testConstruction() {
        List<DataFragment> fragments = new ArrayList<>();
        fragments.add(new SingleItemData<>("Header", type1));
        List<String> test = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            test.add("List" + i);
        fragments.add(new ListData<>(test, type2));
        fragments.add(new SingleItemData<>("Footer", type1));

        DataMerger merger = new DataMerger(fragments);
        assertEquals(0, (int) merger.getStartIndexes().get(0));
        assertEquals(1, (int) merger.getStartIndexes().get(1));
        assertEquals(51, (int) merger.getStartIndexes().get(2));
        assertEquals(0, merger.getFragmentAt(0));
        assertEquals(1, merger.getFragmentAt(1));
        assertEquals(1, merger.getFragmentAt(50));
        assertEquals(2, merger.getFragmentAt(51));

        assertEquals(50 + 2, merger.getItemCount());
        assertEquals("Header", merger.getItem(0));
        assertEquals("Footer", merger.getItem(51));
        for (int i = 0; i < 50; i++)
            assertEquals(test.get(i), merger.getItem(i + 1));

        assertEquals(type1, merger.getHolderTypeFor(0));
        for (int i = 0; i < 50; i++)
            assertEquals(type2, merger.getHolderTypeFor(i + 1));
        assertEquals(type1, merger.getHolderTypeFor(51));
    }

    @Test
    public void testContentUpdate() {
        List<DataFragment> fragments = new ArrayList<>();
        fragments.add(new SingleItemData<>("Header", type1));
        List<String> test = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            test.add("List" + i);
        ListData<String> testData = new ListData<>(test, type2);
        fragments.add(testData);
        fragments.add(new SingleItemData<>("Footer", type1));

        DataMerger merger = new DataMerger(fragments);

        test.add(10, "-- Inserted Element --");
        test.add(11, "-- Inserted Element 2 --");
        testData.notifyItemRangeInserted(10, 2);

        assertEquals(0, (int) merger.getStartIndexes().get(0));
        assertEquals(1, (int) merger.getStartIndexes().get(1));
        assertEquals(53, (int) merger.getStartIndexes().get(2));

        assertEquals(50 + 2, testData.getItemCount());
        assertEquals(50 + 2 + 2, merger.getItemCount());
        assertEquals("Header", merger.getItem(0));
        assertEquals("Footer", merger.getItem(51 + 2));
        for (int i = 0; i < 52; i++)
            assertEquals(test.get(i), merger.getItem(i + 1));
    }

    @Test
    public void testFragmentInsert() {
        List<DataFragment> fragments = new ArrayList<>();
        fragments.add(new SingleItemData<>("Header", type1));
        List<String> test = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            test.add("List" + i);
        ListData<String> testData = new ListData<>(test, type2);
        fragments.add(new SingleItemData<>("Footer", type1));

        DataMerger merger = new DataMerger(fragments);
        fragments.add(1, testData);
        merger.notifyFragmentInserted(1);

        assertEquals(0, (int) merger.getStartIndexes().get(0));
        assertEquals(1, (int) merger.getStartIndexes().get(1));
        assertEquals(51, (int) merger.getStartIndexes().get(2));

        assertEquals(50 + 2, merger.getItemCount());
        assertEquals("Header", merger.getItem(0));
        assertEquals("Footer", merger.getItem(51));
        for (int i = 0; i < 50; i++)
            assertEquals(test.get(i), merger.getItem(i + 1));
    }
}