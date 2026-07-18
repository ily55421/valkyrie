package valkyrie.app.widgets;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.*;

/**
 * @author Luo Tiansheng
 * @since 2026/4/1
 */
public class VkTabPane extends TabPane implements ObservableList<Tab>
{
        /**
         * 构造函数
         */
        public VkTabPane()
        {
                setTabDragPolicy(TabDragPolicy.REORDER);
                setupTabContextMenu();
        }

        private void setupTabContextMenu()
        {
                // Add context menu to each tab when added
                getTabs().addListener((ListChangeListener<Tab>) c -> {
                        while (c.next()) {
                                if (c.wasAdded()) {
                                        for (Tab tab : c.getAddedSubList()) {
                                                tab.setContextMenu(createTabContextMenu(tab));
                                        }
                                }
                        }
                });
        }

        private ContextMenu createTabContextMenu(Tab tab)
        {
                ContextMenu menu = new ContextMenu();

                MenuItem closeOthers = new MenuItem("关闭其它");
                closeOthers.setOnAction(e -> {
                        int idx = getTabs().indexOf(tab);
                        // Close tabs to the right
                        removeExcept(tab, idx + 1, getTabs().size());
                        // Close tabs to the left
                        removeExcept(tab, 0, idx);
                });

                MenuItem closeLeft = new MenuItem("关闭左侧");
                closeLeft.setOnAction(e -> {
                        int idx = getTabs().indexOf(tab);
                        removeExcept(tab, 0, idx);
                });

                MenuItem closeRight = new MenuItem("关闭右侧");
                closeRight.setOnAction(e -> {
                        int idx = getTabs().indexOf(tab);
                        removeExcept(tab, idx + 1, getTabs().size());
                });

                MenuItem closeAll = new MenuItem("关闭全部");
                closeAll.setOnAction(e -> getTabs().clear());

                MenuItem copyTab = new MenuItem("复制 Tab");
                copyTab.setOnAction(e -> {
                        // Copy tab: select current tab (content-specific copy handled by tab content)
                        getSelectionModel().select(tab);
                });

                SeparatorMenuItem sep = new SeparatorMenuItem();

                // Pin/unpin
                MenuItem pinTab = new MenuItem(tab.isClosable() ? "固定 Tab" : "取消固定");
                pinTab.setOnAction(e -> {
                        boolean pinned = !tab.isClosable();
                        tab.setClosable(pinned);
                        pinTab.setText(pinned ? "固定 Tab" : "取消固定");
                        if (!pinned) {
                                // Move pinned tab to front
                                getTabs().remove(tab);
                                getTabs().add(0, tab);
                                getSelectionModel().select(tab);
                        }
                });

                menu.getItems().addAll(closeOthers, closeLeft, closeRight, closeAll, sep, pinTab);
                return menu;
        }

        /// ///////////////////////////////////////////////////////////
        /// 增强函数
        /// ///////////////////////////////////////////////////////////

        public void select(Tab tab)
        {
                getSelectionModel().select(tab);
        }

        private void callOnCloseRequest(Tab tab)
        {
                callOnCloseRequest(null, tab);
        }

        private void callOnCloseRequest(Tab except, Tab tab)
        {
                if (Objects.equals(except, tab))
                        return;

                EventHandler<Event> onCloseRequest = tab.getOnCloseRequest();
                if (onCloseRequest != null)
                        onCloseRequest.handle(new Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT));

                Event.fireEvent(tab, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
        }

        public void addAndSelect(Tab tab)
        {
                add(tab);
                select(tab);
        }

        /// ///////////////////////////////////////////////////////////
        /// ObservableList
        /// ///////////////////////////////////////////////////////////

        @Override
        public void addListener(ListChangeListener<? super Tab> listener)
        {
                getTabs().addListener(listener);
        }

        @Override
        public void removeListener(ListChangeListener<? super Tab> listener)
        {
                getTabs().removeListener(listener);
        }

        @Override
        public boolean addAll(Tab... elements)
        {
                return getTabs().addAll(elements);
        }

        @Override
        public boolean setAll(Tab... elements)
        {
                return getTabs().addAll(elements);
        }

        @Override
        public boolean setAll(Collection<? extends Tab> col)
        {
                return getTabs().addAll(col);
        }

        @Override
        public boolean removeAll(Tab... elements)
        {
                return getTabs().removeAll(elements);
        }

        @Override
        public boolean retainAll(Tab... elements)
        {
                return getTabs().retainAll(elements);
        }

        @Override
        public void remove(int from, int to)
        {
                subList(from, to).forEach(this::callOnCloseRequest);
                getTabs().remove(from, to);
        }

        /** 范围移除，除了 except */
        public void removeExcept(Tab except, int from, int to)
        {
                subList(from, to).forEach(tab -> this.callOnCloseRequest(except, tab));
                getTabs().remove(from, to);
        }

        @Override
        public int size()
        {
                return getTabs().size();
        }

        @Override
        public boolean isEmpty()
        {
                return getTabs().isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
                return getTabs().contains(o);
        }

        @Override
        public Iterator<Tab> iterator()
        {
                return getTabs().iterator();
        }

        @Override
        public Object[] toArray()
        {
                return getTabs().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
                return getTabs().toArray(a);
        }

        @Override
        public boolean add(Tab tab)
        {
                return getTabs().add(tab);
        }

        @Override
        public boolean remove(Object o)
        {
                if (o instanceof Tab tab)
                        callOnCloseRequest(tab);
                return getTabs().remove(o);
        }

        @Override
        @SuppressWarnings("SlowListContainsAll")
        public boolean containsAll(Collection<?> c)
        {
                return getTabs().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Tab> c)
        {
                return getTabs().addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends Tab> c)
        {
                return getTabs().addAll(index, c);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
                return getTabs().removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
                return getTabs().retainAll(c);
        }

        @Override
        public void clear()
        {
                getTabs().clear();
        }

        @Override
        public Tab get(int index)
        {
                return getTabs().get(index);
        }

        @Override
        public Tab set(int index, Tab element)
        {
                return getTabs().set(index, element);
        }

        @Override
        public void add(int index, Tab element)
        {
                getTabs().add(index, element);
        }

        @Override
        public Tab remove(int index)
        {
                callOnCloseRequest(get(index));
                return getTabs().remove(index);
        }

        @Override
        public int indexOf(Object o)
        {
                return getTabs().indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o)
        {
                return getTabs().lastIndexOf(o);
        }

        @Override
        public ListIterator<Tab> listIterator()
        {
                return getTabs().listIterator();
        }

        @Override
        public ListIterator<Tab> listIterator(int index)
        {
                return getTabs().listIterator(index);
        }

        @Override
        public List<Tab> subList(int fromIndex, int toIndex)
        {
                return getTabs().subList(fromIndex, toIndex);
        }

        @Override
        public void addListener(InvalidationListener listener)
        {
                getTabs().addListener(listener);
        }

        @Override
        public void removeListener(InvalidationListener listener)
        {
                getTabs().removeListener(listener);
        }
}
