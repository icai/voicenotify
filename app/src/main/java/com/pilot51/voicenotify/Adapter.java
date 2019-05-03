package com.pilot51.voicenotify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends BaseAdapter implements Filterable {
    private final List<App> baseData = new ArrayList<>();
    private final List<App> adapterData = new ArrayList<>();
    private final LayoutInflater mInflater;
    private SimpleFilter filter;

    public Adapter(Context context) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<App> list) {
        baseData.clear();
        baseData.addAll(list);
        refresh();
    }

    public int getPositionForSection(int section) {
        for (int i = 0; i < getCount(); i++) {
            String sortStr = baseData.get(i).getSortLetters();
            char firstChar = sortStr.toUpperCase().charAt(0);
            if (firstChar == section) {
                return i;
            }
        }
        return -1;
    }


    private void refresh() {
        adapterData.clear();
        adapterData.addAll(baseData);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return adapterData.size();
    }

    @Override
    public Object getItem(int position) {
        return adapterData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder {
        private TextView appLabel;
        private TextView appPackage;
        private CheckBox checkbox;
        private ImageView appIcon;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.app_list_item, parent, false);
            holder = new ViewHolder();
            holder.appLabel = view.findViewById(R.id.app_label);
            holder.appPackage = view.findViewById(R.id.app_package);
            holder.checkbox = view.findViewById(R.id.checkbox);
            holder.appIcon = view.findViewById(R.id.app_icon);
            view.setTag(holder);
        } else {
            holder = (ViewHolder)view.getTag();
        }
        holder.appLabel.setText(adapterData.get(position).getLabel());
        holder.appPackage.setText(adapterData.get(position).getPackage());
        holder.appIcon.setImageDrawable(adapterData.get(position).getAppIcon());
        holder.appIcon.setContentDescription(adapterData.get(position).getLabel());
        holder.checkbox.setChecked(adapterData.get(position).getEnabled());
        return view;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) filter = new SimpleFilter();
        return filter;
    }

    private class SimpleFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (prefix == null || prefix.length() == 0) {
                results.values = baseData;
                results.count = baseData.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                List<App> newValues = new ArrayList<>();
                for (App app : baseData) {
                    if (app.getLabel().toLowerCase().contains(prefixString)
                            || app.getPackage().toLowerCase().contains(prefixString)) {
                        newValues.add(app);
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapterData.clear();
            adapterData.addAll((List<App>)results.values);
            if (results.count > 0) notifyDataSetChanged();
            else notifyDataSetInvalidated();
        }
    }
}
