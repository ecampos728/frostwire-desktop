package com.frostwire.gui.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.limewire.util.FileUtils;
import org.pushingpixels.substance.api.ColorSchemeAssociationKind;
import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTreeCellRenderer;
import org.pushingpixels.substance.internal.ui.SubstanceTreeUI;
import org.pushingpixels.substance.internal.ui.SubstanceTreeUI.TreePathId;
import org.pushingpixels.substance.internal.utils.SubstanceColorSchemeUtilities;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.trees.FileTreeModel;
import com.limegroup.gnutella.settings.LibrarySettings;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Provides a tree panel of a partial view of the filesystem. Folders in the
 * tree view can be selected and deselected which marks if they are going
 * to be included or not.
 */
// Implementation caveat: Before removing items from the tree, invoke
// direcotoryTree.cancelEditing() to avoid a null pointer exception in the 
// repaint code
public class RecursiveLibraryDirectoryPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 2342466847251833757L;

    private final FileTreeModel directoryTreeModel;
    
    private final JTree directoryTree;
    /**
     * Set of directories that are not to be excluded.
     */
    private final Set<File> deselected;
    /**
     * The set of root folders of this partial view on the filesystem.
     */
    private final Set<File> roots;
    
    private final JPanel legendPanel;
    
    private final JPanel mainPanel;
    
    /**
     * Cell renderer for the tree that uses a check box for rendering of file tree
     * data. Kept around here so its color configurations can be used.
     */
    private final FileTreeCellRenderer fileTreeCellRenderer = new FileTreeCellRenderer();
    
    /**
     * The checkbox icon that represents the third possible state, a folder whose
     * files are being included but only some of its subfolders. 
     */
    private final Icon partiallyIncludedIcon = createPartiallyIncludedIcon();
    
    private static Set<File> emptyFileSet() {
        return Collections.emptySet();
    }
    
    private static Icon createPartiallyIncludedIcon() {
        if (ThemeSettings.isNativeOSXTheme()) {
            return GUIMediator.getThemeImage("sharing_checkbox_aqua");
        } else {
            Icon icon = UIManager.getIcon("CheckBox.icon");
            if (icon == null || icon.getIconWidth() == 0 || icon.getIconHeight() == 0) {
                icon = MetalIconFactory.getCheckBoxIcon();
            }
            return createDisabledIcon(configureCheckBox(new JCheckBox()), icon);
        }
    }
    
    /**
     * Constructs the tree view with a list of roots, can be null. 
     */
    public RecursiveLibraryDirectoryPanel(boolean precheckFolders, File... roots) {
        this(precheckFolders, emptyFileSet(), roots);
    }
    
    /**
     * Constructs the tree view with a list of roots.
     * @param blackListSet set of subfolders that are marked as not included, cannot be null
     * @param roots list of roots, can be null
     */
    public RecursiveLibraryDirectoryPanel(boolean precheckFolders,Set<File> blackListSet, File... roots) {
        super(new BorderLayout());
        this.roots = new TreeSet<File>(FileTreeModel.DEFAULT_COMPARATOR);
        this.deselected = new HashSet<File>(blackListSet);
        
        if (!precheckFolders) {
        	addFoldersToExclude(new HashSet<File>(Arrays.asList(roots)));
        }

        // center
        directoryTreeModel = new FileTreeModel("");
        directoryTreeModel.setFileFilter(new IncludedFolderFilter());
        
        directoryTree = new RootNotEditableTree(directoryTreeModel);
        directoryTree.setBorder(new EmptyBorder(4, 4, 4, 4));
        directoryTree.setCellRenderer(fileTreeCellRenderer);
        directoryTree.setCellEditor(new FileTreeCellEditor());
        directoryTree.setEditable(true);
        directoryTree.setVisibleRowCount(8);
        directoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        JScrollPane jspDirectories = new JScrollPane(directoryTree);
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(jspDirectories, BorderLayout.CENTER);
        legendPanel = new JPanel();
        mainPanel.add(legendPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        setRoots(roots);
        
        updateLanguage();
    }
    
    /** Resets the text of various components to the current language. */
    public void updateLanguage() {
        createLegendPanel(legendPanel);
        directoryTreeModel.changeRootText(I18n.tr("Library Folders"));
    }
    
    /**
     * Adds a panel at the eastern side of the main panel.
     * 
     * A button panel or the like can be plugged in thusly.
     */
    public void addEastPanel(JComponent comp) {
        mainPanel.add(comp, BorderLayout.EAST);
    }
    
    /**
     * Sets the list of roots, old roots will be cleared. 
     */
    public void setRoots(File... newRoots) {
        this.roots.clear();
        // call cancel editing before tree is cleared
        directoryTree.cancelEditing();
        directoryTreeModel.removeSubRoots();
        this.roots.addAll(retainAncestors(newRoots));
        for (File root : roots) {
            directoryTreeModel.addSubRoot(root);
        }
        setRootExpanded();
    }
    
    /**
     * Returns true if <code>dir</code> is on of the currenlty included roots.
     */
    public boolean isRoot(File dir) {
        return roots.contains(dir);
    }
    
    /**
     * Sets the set of subfolders to mark as not included. 
     */
    public void setFoldersToExclude(Set<File> blackListSet) {
        deselected.clear();
        deselected.addAll(blackListSet);
    }
    
    /**
     * Adds <code>blackListSet</code> to the set of black folders to exclude
     * from sharing. 
     */
    public void addFoldersToExclude(Set<File> blackListSet) {
        deselected.addAll(blackListSet);
    }

    /**
     * Expand the root nodes.
     */
    public void setRootsExpanded() {
        for (File root : roots) {
            setExpanded(root);
        }
    }
    
    /**
     * Expand the visible or invisible root node.
     */
    public void setRootExpanded() {
        directoryTree.expandPath(new TreePath(directoryTreeModel.getRoot()));
    }
    
    /**
     * Adds a root to the tree if it's not already in there.
     * 
     * @return false if dir is already in the tree
     */
    public boolean addRoot(File dir) {
        // remove from deselected in any case
        boolean changed = deselected.remove(dir);
        // check if already included
        for (File root : roots) {
            if (FileUtils.isAncestor(root, dir)) {
                if (root.equals(dir)) {
                    if (changed) {
                        directoryTreeModel.valueForPathChanged(getTreePath(dir), null);
                    }
                    return changed;
                }
                // make sure it is included
                removeFromPath(dir);
                TreePath path = getTreePath(dir);
                directoryTree.scrollPathToVisible(path);
                return changed;
            }
            else if (FileUtils.isAncestor(dir, root)) {
                removeRoot(root);
                addDirToTree(dir);
                // expand to root and its parent, since expand has no effect if root
                // doesn't have subfolders
                setExpanded(root);
                // guaranteed to not be null since at least dir is its real ancestor
                setExpanded(root.getParentFile());
                return true;
            }
        }
        addDirToTree(dir);
        setRootExpanded();
        return true;
    }
    
    private void addDirToTree(File dir) {
        roots.add(dir);
        directoryTreeModel.addSubRoot(dir);
    }
    
    /**
     * Removes <code>root</code> from the tree of included roots. 
     */
    public void removeRoot(File root) {
        if (roots.remove(root)) {
            // remove old root and add dir and expand to root
            // cancel editing, since we're removing from the tree
            directoryTree.cancelEditing();
            directoryTreeModel.removeSubRoot(root);
            removeFromPath(root);
        }
    }
    
    /**
     * Retains common ancestors and removes subfolders since they will be part
     * of the recursive sharing. 
     */
    static Set<File> retainAncestors(File... roots) {
        if (roots == null) {
            return new HashSet<File>();
        }
        for (int i = 0; i < roots.length; i++) {
            for (int j = i + 1; j < roots.length && roots[i] != null; j++) {
                if (roots[j] != null) {
                    if (FileUtils.isAncestor(roots[i], roots[j])) {
                        roots[j] = null;
                    }
                    else if (FileUtils.isAncestor(roots[j], roots[i])) {
                        roots[i] = null;
                    }
                }
            }
        }
        Set<File> retained = new HashSet<File>(roots.length);
        for (File file : roots) {
            if (file != null) {
                retained.add(file);
            }
        }
        return retained;
    }
    
    /**
     * Create the legend  
     * 
     * If basePanel is non-null, the legend panel replaces the contents of that panel.
     */
    private JPanel createLegendPanel(JPanel basePanel) {
        final JPanel panel;
        if(basePanel != null) {
            basePanel.removeAll();
            basePanel.setLayout(new GridBagLayout());
            panel = basePanel;
        } else {
            panel = new JPanel(new GridBagLayout());
        }

        panel.setBorder(BorderFactory.createTitledBorder(I18n.tr("Legend")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 4, 0, 6);
        
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(true);
        panel.add(createIconLabel(checkBox), gbc);
        
        MultiLineLabel label = new MultiLineLabel(I18n.tr("Folder and subfolders are included in the Library."), true);
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.fill = GridBagConstraints.HORIZONTAL;
        labelGbc.gridwidth = GridBagConstraints.REMAINDER;
        labelGbc.gridx = 1;
        labelGbc.weightx = 1;
        
        panel.add(label, labelGbc);
        
        checkBox.setSelected(false);
        gbc.gridy = 1;
        panel.add(createIconLabel(checkBox), gbc);

        label = new MultiLineLabel(I18n.tr("Folder is not included and no subfolders are included in the Library."), true);
        labelGbc.gridy = 1;
        panel.add(label, labelGbc);
        
        checkBox.setIcon(partiallyIncludedIcon);
        gbc.gridy = 2;
        panel.add(createIconLabel(checkBox), gbc);
        
        label = new MultiLineLabel(I18n.tr("Folder\'s files and some subfolders are included in the Library."), true);
        labelGbc.gridy = 2;
        panel.add(label, labelGbc);
        
        return panel;
    }
   
    /**
     * Creates an image of the checkbox and puts it in a label. 
     */
    private JLabel createIconLabel(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setSize(checkBox.getMinimumSize());
        Image image = new BufferedImage(checkBox.getWidth(), checkBox.getHeight(), Transparency.TRANSLUCENT);
        Graphics g = image.getGraphics();
        checkBox.paint(g);
        g.dispose();
        return new JLabel(new ImageIcon(image));
    }
    
    /**
     * Returns the set of folder to include.
     *      
     * Deselected root folders are not returned.
     */
    public Set<File> getRootsToInclude() {
        Set<File> ret = new HashSet<File>(roots);
        ret.removeAll(deselected);
        return ret;
    }
    
    /**
     * Returns the set of subfolder to exclude from sharing.
     * 
     * Deselected root folders are not returned. Why???
     */
    public Set<File> getFoldersToExclude() {
        Set<File> result = new HashSet<File>(deselected);
        //result.removeAll(roots);
        return result;
    }
    
    /**
     * Constructs the tree path for a directory.
     */
    private TreePath getTreePath(File dir) {
        LinkedList<File> files = new LinkedList<File>();
        files.add(dir);
        File parent = dir.getParentFile();
        while (parent != null && !directoryTreeModel.isSubRoot(dir)) {
            files.addFirst(parent);
            dir = parent;
            parent = parent.getParentFile();
        }
        Object[] path = new Object[files.size() + 1];
        path[0] = directoryTreeModel.getRoot();
        System.arraycopy(files.toArray(), 0, path, 1, path.length - 1);
        return new TreePath(path);   
    }

    /**
     * Returns the tree component. 
     */
    public JTree getTree() {
        return directoryTree;
    }
    
    /**
     * Expands node in in the tree corresponding to <code>dir</code>.  
     */
    public void setExpanded(File dir)
    {
        directoryTree.expandPath(getTreePath(dir));
    }
    
    /**
     * Configures a checkbox with the properties of tree cell renderes. 
     */
    private static JCheckBox configureCheckBox(JCheckBox checkBox) {
        checkBox.setHorizontalAlignment(JCheckBox.LEFT);
        checkBox.setFont(UIManager.getFont("Tree.font"));
        checkBox.setBorderPainted(false);
        checkBox.setFocusPainted(false);
        checkBox.setFocusable(false);
        checkBox.setOpaque(false);
        return checkBox;
    }
    
    /**
     * Returns true if <code>dir</code> is included and all of its parents 
     * are included. 
     */
    private boolean isIncludedOrParentIsIncluded(File dir) {
        while (dir != null) {
            if (deselected.contains(dir)) {
                return false;
            }
            dir = dir.getParentFile();
        }
        return true;
    }
    
    /**
     * Returns true if dir is excluded from sharing or one of its ancestors is. 
     */
    private boolean isExcluded(File dir) {
        for (File file : deselected) {
            if (FileUtils.isAncestor(file, dir)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if dir and all its subfolders are included. 
     */
    private boolean isFullyIncluded(File dir) {
        for (File offspring : deselected) {
            if (FileUtils.isAncestor(dir, offspring)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creates a disabled checkbox icon. 
     */
    private static Icon createDisabledIcon(JCheckBox checkBox, Icon icon) {
        Image image = new BufferedImage(icon.getIconWidth(), icon.getIconWidth(), BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        checkBox.setSelected(true);
        icon.paintIcon(checkBox, g, 0, 0);
        g.dispose();
        return UIManager.getLookAndFeel().getDisabledIcon(checkBox, new ImageIcon(image));
    }
    
    /**
     * Returns the text used by the tree cell renderers. 
     */
    private String getText(File file) {
        return directoryTreeModel.isSubRoot(file) ? file.getAbsolutePath() : file.getName();
    }
    
    /**
     * Removes all files from <code>deselected</code> that are ancestors
     * of <code>file</code or vice versa.
     */
    private void removeFromPath(File file) {
        // remove all children, file itself is removed here too
        for (Iterator<File> i = deselected.iterator(); i.hasNext();) {
            File f = i.next();
            if (FileUtils.isAncestor(file, f)) {
                i.remove();
            }
        }
        while (file != null && !roots.contains(file)) {
            // mark other children of parent as excluded
            File parent = file.getParentFile();
            if (ancestorIsExcluded(parent)) {
                deselected.remove(parent);
                int childCount = directoryTreeModel.getChildCount(parent);
                for (int j = 0; j < childCount; j++) {
                    File sibling = (File)directoryTreeModel.getChild(parent, j);
                    if (sibling != null && !sibling.equals(file)) {
                        deselected.add(sibling);
                    }
                }
            }
            file = parent;
        }
    }
    
    private boolean ancestorIsExcluded(File file) {
        while (file != null) {
            if (deselected.contains(file)) {
                return true;
            }
            file = file.getParentFile();
        }
        return false;
    }
    
    /**
     * Sets the colors of the checkbox depending on its state.
     */
    private void setColors(Component colorExample, JTree tree, TreePath path, JCheckBox checkBox, boolean isSelected) {
        if (SubstanceLookAndFeel.isCurrentLookAndFeel()) {
            checkBox.setForeground(getSubstanceForegroundColor(tree, path));
            checkBox.setBackground(getSubstanceBackgroundColor(tree, path));
            checkBox.setOpaque(false);
        } else {
            checkBox.setForeground(colorExample.getForeground());
            checkBox.setBackground(colorExample.getBackground());
            checkBox.setOpaque(false);
        }
    }
    
    /**
     * Check box tree cell renderer.
     */
    private class FileTreeCellRenderer extends SubstanceDefaultTreeCellRenderer {
        
        private static final long serialVersionUID = -8299879264709364378L;
        
        private JCheckBox checkBox = configureCheckBox(new JCheckBox());
        
        private SubstanceDefaultTreeCellRenderer labelRenderer = new SubstanceDefaultTreeCellRenderer();
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                    row, hasFocus);
            Component compTemp = null;
            if (!(value instanceof File)) {
                labelRenderer.getTreeCellRendererComponent(tree, value, sel, expanded, false, row, false);
                labelRenderer.setIcon(null);
                return labelRenderer;
            } else {
                compTemp = labelRenderer.getTreeCellRendererComponent(tree, RecursiveLibraryDirectoryPanel.this.getText((File)value), sel, expanded, false, row, false);
            }
            
            File file = (File)value;
            checkBox.setText(RecursiveLibraryDirectoryPanel.this.getText(file));
            setColors(compTemp, tree, tree.getPathForRow(row), checkBox, sel);
            
            if (isExcluded(file)) {
                checkBox.setSelected(false);
                checkBox.setIcon(null);
            } else if (isFullyIncluded(file)) {
                checkBox.setSelected(true);
                checkBox.setIcon(null);
            } else {
                checkBox.setSelected(true);
                checkBox.setIcon(partiallyIncludedIcon);
            }
            return checkBox;
        }
    }
    
    /**
     * Checkbox tree cell editor.
     */
    private class FileTreeCellEditor extends DefaultCellEditor {

                
        /**
         * 
         */
        private static final long serialVersionUID = -8422311328409412824L;

        public FileTreeCellEditor() {
            super(configureCheckBox(new JCheckBox()));
            //setColors((JCheckBox)editorComponent, true);
            
            delegate = new EditorDelegate() {
                
                private static final long serialVersionUID = -7007164079287676831L;
                
                @Override
                public void setValue(Object value) {
                    File file = (File)value;
                    ((JCheckBox)editorComponent).setSelected(isIncludedOrParentIsIncluded(file));
                    ((JCheckBox)editorComponent).setText(RecursiveLibraryDirectoryPanel.this.getText(file));
                    this.value = value;
                }
                
                @Override
                public Object getCellEditorValue() {
                    final File file = (File)value;
                    if (((JCheckBox)editorComponent).isSelected()) {
                        removeFromPath(file);
                    } else {
                        deselected.add(file);
                    }
                    return this.value;
                }
            };
        }
        
        /**
         * Overridden to pass the value to the delegate and not just its
         * string representation as done in the super class.
         */
        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value,
                boolean isSelected, boolean expanded, boolean leaf, int row) {
            delegate.setValue(value);
            return editorComponent;
        }
        
    }

    /**
     * Overriden to set the root path to not be editable.
     */
    private static class RootNotEditableTree extends JTree {

        /**
         * 
         */
        private static final long serialVersionUID = 3856730985269585441L;

        public RootNotEditableTree(TreeModel newModel) {
            super(newModel);
        }
        
        @Override
        public boolean isPathEditable(TreePath path) {
            Object comp = path.getLastPathComponent();
            if (comp instanceof File) {
                if (comp.equals(SharingSettings.TORRENT_DATA_DIR_SETTING.getValue())) {
                    return false;
                }
            }            
            
            // root node is not editable
            return path.getPathCount() != 1;
        }
    }

    private static class IncludedFolderFilter implements FileFilter {
        public boolean accept(File pathname) {
            if (FileUtils.isAncestor(SharingSettings.TORRENT_DATA_DIR_SETTING.getValue(), pathname)) {
                return false;
            }
            
            for (File f : LibrarySettings.DIRECTORIES_TO_INCLUDE_FROM_FROSTWIRE4.getValue()) {
                if (FileUtils.isAncestor(f, pathname)) {
                    return false;
                }
            }
            
            if (FileUtils.isAncestor(LibrarySettings.USER_MUSIC_FOLDER.getValue(), pathname)) {
                return false;
            }
            
            return pathname.isDirectory() && !pathname.isHidden();
        }
    };
    
    private Color getSubstanceForegroundColor(JTree tree, TreePath path) {
        TreeUI tableUI = tree.getUI();
        SubstanceTreeUI ui = (SubstanceTreeUI) tableUI;
        TreePathId pathId = new TreePathId(path);
        ComponentState currState = ui.getPathState(pathId);

        SubstanceColorScheme scheme = getColorSchemeForState(tree, ui, currState);

        return scheme.getForegroundColor();
    }
    
    private Color getSubstanceBackgroundColor(JTree tree, TreePath path) {
        TreeUI tableUI = tree.getUI();
        SubstanceTreeUI ui = (SubstanceTreeUI) tableUI;
        TreePathId pathId = new TreePathId(path);
        ComponentState currState = ui.getPathState(pathId);

        SubstanceColorScheme scheme = getColorSchemeForState(tree, ui, currState);

        return scheme.getBackgroundFillColor();
    }

    private SubstanceColorScheme getColorSchemeForState(JTree tree, SubstanceTreeUI ui, ComponentState state) {
        if (state == ComponentState.ENABLED) {
            return SubstanceColorSchemeUtilities.getColorScheme(tree, state);
        } else {
            return SubstanceColorSchemeUtilities.getColorScheme(tree, ColorSchemeAssociationKind.HIGHLIGHT, state);
        }
    }
}