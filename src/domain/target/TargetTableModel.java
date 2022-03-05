package domain.target;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import com.google.common.net.InternetDomainName;

import burp.BurpExtender;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;

public class TargetTableModel extends AbstractTableModel {

	private IndexedLinkedHashMap<String,TargetEntry> targetEntries =new IndexedLinkedHashMap<String,TargetEntry>();
	private boolean ListenerIsOn = true;

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] standardTitles = new String[] {
			"Domain/Subnet/IP", "Keyword", "Comments"};
	private static List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));
	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}
	public TargetTableModel(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		/**
		 * 
		 * 注意，所有直接对TargetTableModel中数据的修改，都不会触发该tableChanged监听器。
		 * 除非操作的逻辑中包含了firexxxx来主动通知监听器。
		 * DomainPanel.domainTableModel.fireTableChanged(null);
		 */
		addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ListenerIsOn) {
					DomainPanel.autoSave();
				}
			}
		});
	}

	//getter setter是为了序列化和反序列化
	public IndexedLinkedHashMap<String, TargetEntry> getTargetEntries() {
		return targetEntries;
	}
	public void setTargetEntries(IndexedLinkedHashMap<String, TargetEntry> targetEntries) {
		this.targetEntries = targetEntries;
	}

	@Override
	public int getRowCount() {
		return targetEntries.size();
	}

	@Override
	public int getColumnCount() {
		return standardTitles.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TargetEntry entry = targetEntries.get(rowIndex);
		if (columnIndex == titletList.indexOf("Domain/Subnet/IP")) {
			return entry.getTarget();
		}
		if (columnIndex == titletList.indexOf("Keyword")) {
			return entry.getKeyword();
		}
		if (columnIndex == titletList.indexOf("Comments")) {
			return entry.getComment();
		}
		return "";
	}

	public void clear() {
		for (int i=0;i<targetEntries.size();i++) {
			removeRow(i);
		}
	}

	/**
	 * 用于加载数据时，直接初始化
	 * @param targetEntries
	 */
	public void setData(IndexedLinkedHashMap<String,TargetEntry> targetEntries) {
		clear();
		this.targetEntries = targetEntries;
		int size = targetEntries.size();
		if (size >=1) {
			fireTableRowsInserted(0, size-1);
		}
	}

	/**
	 * 数据的增删查改：新增
	 * @param key
	 * @param entry
	 */
	public void addRow(String key,TargetEntry entry) {
		int oldsize = targetEntries.size();
		targetEntries.put(key,entry);
		int newsize = targetEntries.size();
		if (oldsize ==newsize) {
			int rowIndex = targetEntries.IndexOfKey(key);
			fireTableRowsUpdated(rowIndex,rowIndex);
		}else {
			int rowIndex = getRowCount();
			fireTableRowsInserted(rowIndex, rowIndex);
		}
	}

	/**
	 * 数据的增删查改：删除
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex) {
		targetEntries.remove(rowIndex);
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	/**
	 * 数据的增删查改：删除
	 */
	public void removeRow(String key) {
		int rowIndex = targetEntries.IndexOfKey(key);
		targetEntries.remove(key);
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	/**
	 * 数据的增删查改：查询
	 */
	public TargetEntry getValueAt(int rowIndex) {
		TargetEntry entry = targetEntries.get(rowIndex);
		return entry;
	}

	/**
	 * 数据的增删查改：修改更新
	 */
	public void updateRow(TargetEntry entry) {
		String key = entry.getTarget();
		addRow(key,entry);
	}



	/**
	 * 获取数据集的方法
	 * @return
	 */
	public String fetchRootDomains() {
		return String.join(System.lineSeparator(), targetEntries.keySet());
	}


	public Set<String> fetchRootDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!entry.isBlack()) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	public Set<String> fetchRootBlackDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (entry.isBlack()) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	public Set<String> fetchKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!entry.isBlack() && !entry.getKeyword().trim().equals("")) {
				result.add(entry.getKeyword());
			}
		}
		return result;
	}

	/**
	 * 主要用于package name的有效性判断
	 * @return
	 */
	public Set<String> fetchSuffixSet(){
		Set<String> result = new HashSet<String>();
		for (String key:targetEntries.keySet()) {
			String suffix;
			try {
				//InternetDomainName.from(key).publicSuffix() //当不是com、cn等公共的域名结尾时，将返回空。
				suffix = InternetDomainName.from(key).publicSuffix().toString();
			} catch (Exception e) {
				suffix = key.split("\\.",2)[1];//分割成2份
			}
			result.add(suffix);
		}
		return result;
	}

}
