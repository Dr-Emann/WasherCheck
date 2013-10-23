/*
 * Copyright (c) 2013. Zachary Dremann
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.zdremann.wc.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.zdremann.wc.io.locations.LocationsProxy;
import net.zdremann.wc.model.MachineGrouping;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.Lazy;

public class RoomChooserFragment extends InjectingListFragment {
    private long mRootId;
    private MachineGroupingAdapter mAdapter;
    private MachineGrouping mRoot;
    private RoomChooserListener mChooseListener;

    @Inject
    Lazy<LayoutInflater> mLayoutInflater;

    @Inject
    Lazy<LocationsProxy> mLocations;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mChooseListener = (RoomChooserListener) activity;

        Bundle args = getArguments();
        mRootId = args.getLong(RoomChooserActivity.ARG_GROUPING_ID, RoomChooserActivity.ROOT_ID);
        mAdapter = new MachineGroupingAdapter();
        setListAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRoot = mLocations.get().getGrouping(mRootId); //TODO: run in separate thread
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MachineGrouping group = (MachineGrouping) mAdapter.getItem(position);
        mChooseListener.onRoomChosen(group);
    }

    public static interface RoomChooserListener {
        public void onRoomChosen(@NotNull MachineGrouping group);
    }

    private class MachineGroupingAdapter extends BaseAdapter {

        public int getCount() {
            if (mRoot == null)
                return 0;
            return mRoot.children.size();
        }

        public Object getItem(int position) {
            final MachineGrouping grouping = mRoot.children.get(position);
            assert grouping != null;
            return grouping;
        }

        public long getItemId(int position) {
            return ((MachineGrouping) getItem(position)).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = mLayoutInflater.get();
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            MachineGrouping group = (MachineGrouping) getItem(position);

            assert convertView != null;
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(group.name);
            return convertView;
        }
    }

    @Override
    public String toString() {
        return "RoomChooserFragment{" +
              "mRootId=" + mRootId +
              '}';
    }
}
