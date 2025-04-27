package io.mrarm.dataadapter;

import java.util.ArrayList;
import java.util.List;

public class ElementPath {

    private final List<Element> path;

    private ElementPath(List<Element> path) {
        this.path = path;
    }

    public DataFragment get(int i) {
        return path.get(i).getFragment();
    }

    public int size() {
        return path.size();
    }

    public int indexOf(DataFragment fragment) {
        int i;
        for (i = path.size() - 1; i >= 0; --i) {
            if (path.get(i).getFragment() == fragment)
                break;
        }
        return i;
    }

    public int getPosition(int of, int relativeTo) {
        if (of < 0)
            of = path.size() + of;
        if (relativeTo < 0)
            relativeTo = path.size() + relativeTo;
        int ret = 0;
        for (int i = relativeTo + 1; i <= of; i++)
            ret += path.get(i).getPosition();
        return ret;
    }

    public int getPosition(int of, DataFragment relativeTo) {
        int relativeToI = indexOf(relativeTo);
        if (relativeToI == -1)
            return -1;
        return getPosition(of, relativeToI);
    }

    public int getIndexInParent(int of) {
        if (of < 0)
            of = path.size() + of;
        return path.get(of).getIndex();
    }


    public static class Builder {

        private ElementPath path = new ElementPath(new ArrayList<>());

        public void add(Element el) {
            path.path.add(el);
        }

        public ElementPath build() {
            ElementPath ret = path;
            path = null;
            return ret;
        }

    }


    public interface Element {

        DataFragment getFragment();

        int getIndex();

        int getPosition();

    }

    public static class SimpleElement implements Element {

        private DataFragment fragment;
        private int index;
        private int position;

        public SimpleElement(DataFragment fragment, int index, int position) {
            this.fragment = fragment;
            this.index = index;
            this.position = position;
        }

        @Override
        public DataFragment getFragment() {
            return fragment;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getPosition() {
            return position;
        }
    }

}
