package com.insightfullogic.honest_profiler.ports.javafx.controller;

import static com.insightfullogic.honest_profiler.ports.javafx.model.filter.FilterType.STRING;
import static com.insightfullogic.honest_profiler.ports.javafx.util.DialogUtil.FILTER;
import static com.insightfullogic.honest_profiler.ports.javafx.util.FxUtil.addProfileNr;
import static com.insightfullogic.honest_profiler.ports.javafx.util.FxUtil.createColoredLabelContainer;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.COLUMN_SELF_PCT;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_BUTTON_COLLAPSEALLALL;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_BUTTON_EXPANDALL;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_BUTTON_FILTER;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_BUTTON_QUICKFILTER;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_INPUT_QUICKFILTER;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil.INFO_TABLE_TREEDIFF;
import static com.insightfullogic.honest_profiler.ports.javafx.util.TreeUtil.expandPartial;
import static com.insightfullogic.honest_profiler.ports.javafx.view.Icon.FUNNEL_16;
import static com.insightfullogic.honest_profiler.ports.javafx.view.Icon.FUNNEL_ACTIVE_16;
import static com.insightfullogic.honest_profiler.ports.javafx.view.Icon.viewFor;
import static com.insightfullogic.honest_profiler.ports.javafx.view.Rendering.renderPercentage;
import static javafx.geometry.Pos.CENTER;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.insightfullogic.honest_profiler.core.filters.Filter;
import com.insightfullogic.honest_profiler.core.filters.ProfileFilter;
import com.insightfullogic.honest_profiler.core.filters.StringFilter;
import com.insightfullogic.honest_profiler.core.profiles.Profile;
import com.insightfullogic.honest_profiler.ports.javafx.controller.filter.FilterDialogController;
import com.insightfullogic.honest_profiler.ports.javafx.model.ApplicationContext;
import com.insightfullogic.honest_profiler.ports.javafx.model.ProfileContext;
import com.insightfullogic.honest_profiler.ports.javafx.model.diff.MethodNodeDiff;
import com.insightfullogic.honest_profiler.ports.javafx.model.diff.TreeNodeDiff;
import com.insightfullogic.honest_profiler.ports.javafx.model.diff.TreeProfileDiff;
import com.insightfullogic.honest_profiler.ports.javafx.model.filter.FilterSpecification;
import com.insightfullogic.honest_profiler.ports.javafx.model.task.CopyAndFilterProfile;
import com.insightfullogic.honest_profiler.ports.javafx.util.DialogUtil;
import com.insightfullogic.honest_profiler.ports.javafx.util.ResourceUtil;
import com.insightfullogic.honest_profiler.ports.javafx.util.TreeUtil;
import com.insightfullogic.honest_profiler.ports.javafx.view.cell.TreeDiffViewCell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class TreeDiffViewController extends ProfileDiffViewController<Profile>
{
    @FXML
    private Button filterButton;
    @FXML
    private Button expandAllButton;
    @FXML
    private Button collapseAllButton;
    @FXML
    private TextField quickFilterText;
    @FXML
    private Button quickFilterButton;
    @FXML
    private TreeTableView<TreeNodeDiff> diffTree;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> methodColumn;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> baseSelfPct;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> newSelfPct;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> selfPctDiff;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> baseTotalPct;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> newTotalPct;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> totalPctDiff;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> baseSelfCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> newSelfCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> selfCountDiff;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> baseTotalCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> newTotalCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> totalCountDiff;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> baseParentCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> newParentCount;
    @FXML
    private TreeTableColumn<TreeNodeDiff, String> parentCountDiff;

    private FilterDialogController filterDialogController;
    private ObjectProperty<FilterSpecification> filterSpec;

    private TreeProfileDiff diff;

    private ProfileFilter currentFilter;
    private StringFilter quickFilter;

    @Override
    @FXML
    protected void initialize()
    {
        super.initialize(profileContext -> profileContext.profileProperty());

        diff = new TreeProfileDiff();

        currentFilter = new ProfileFilter();

        filterDialogController = (FilterDialogController) DialogUtil
            .<FilterSpecification>createDialog(FILTER, "Specify Filters", false);
        filterDialogController.addAllowedFilterTypes(STRING);

        filterButton
            .setOnAction(event -> filterSpec.set(filterDialogController.showAndWait().get()));

        expandAllButton.setOnAction(
            event -> diffTree.getRoot().getChildren().stream().forEach(TreeUtil::expandFully));

        collapseAllButton.setOnAction(
            event -> diffTree.getRoot().getChildren().stream().forEach(TreeUtil::collapseFully));

        quickFilterButton.setOnAction(event -> applyQuickFilter());

        filterSpec = new SimpleObjectProperty<>(null);
        filterSpec.addListener((property, oldValue, newValue) ->
        {
            filterButton.setGraphic(
                newValue == null || !newValue.isFiltering() ? viewFor(FUNNEL_16)
                    : viewFor(FUNNEL_ACTIVE_16));
            currentFilter = new ProfileFilter(newValue.getFilters());
            refresh();
        });

        filterButton.setOnAction(
            event -> filterSpec.set(filterDialogController.showAndWait().get()));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
    {
        super.setApplicationContext(applicationContext);
        filterDialogController.setApplicationContext(appCtx());
    }

    @Override
    public void setProfileContexts(ProfileContext baseContext, ProfileContext newContext)
    {
        super.setProfileContexts(baseContext, newContext);

        methodColumn.setCellValueFactory(
            data -> new ReadOnlyStringWrapper(
                data.getValue() == null ? null : data.getValue().getValue().getName()));
        methodColumn.setCellFactory(col -> new TreeDiffViewCell());

        configurePctColumn(
            baseSelfPct,
            TreeNodeDiff::getBaseSelfPct,
            baseContext(),
            getText(COLUMN_SELF_PCT));
        configurePctColumn(
            newSelfPct,
            TreeNodeDiff::getNewSelfPct,
            newContext(),
            getText(COLUMN_SELF_PCT));
        configurePctColumn(
            selfPctDiff,
            TreeNodeDiff::getSelfPctDiff,
            getText(ResourceUtil.COLUMN_SELF_PCT_DIFF));

        configurePctColumn(
            baseTotalPct,
            TreeNodeDiff::getBaseTotalPct,
            baseContext(),
            getText(ResourceUtil.COLUMN_TOTAL_PCT));
        configurePctColumn(
            newTotalPct,
            TreeNodeDiff::getNewTotalPct,
            newContext(),
            getText(ResourceUtil.COLUMN_TOTAL_PCT));
        configurePctColumn(
            totalPctDiff,
            TreeNodeDiff::getTotalPctDiff,
            getText(ResourceUtil.COLUMN_TOTAL_PCT_DIFF));

        configureCntColumn(
            baseSelfCount,
            TreeNodeDiff::getBaseSelfCount,
            baseContext(),
            getText(ResourceUtil.COLUMN_SELF_CNT));
        configureCntColumn(
            newSelfCount,
            TreeNodeDiff::getNewSelfCount,
            newContext(),
            getText(ResourceUtil.COLUMN_SELF_CNT));
        configureCntDiffColumn(
            selfCountDiff,
            TreeNodeDiff::getSelfCountDiff,
            getText(ResourceUtil.COLUMN_SELF_CNT_DIFF));

        configureCntColumn(
            baseTotalCount,
            TreeNodeDiff::getBaseTotalCount,
            baseContext(),
            getText(ResourceUtil.COLUMN_TOTAL_CNT));
        configureCntColumn(
            newTotalCount,
            TreeNodeDiff::getNewTotalCount,
            newContext(),
            getText(ResourceUtil.COLUMN_TOTAL_CNT));
        configureCntDiffColumn(
            totalCountDiff,
            TreeNodeDiff::getTotalCountDiff,
            getText(ResourceUtil.COLUMN_TOTAL_CNT_DIFF));

        configureCntColumn(
            baseParentCount,
            TreeNodeDiff::getBaseParentCount,
            baseContext(),
            getText(ResourceUtil.COLUMN_PARENT_CNT));
        configureCntColumn(
            newParentCount,
            TreeNodeDiff::getNewParentCount,
            newContext(),
            getText(ResourceUtil.COLUMN_PARENT_CNT));
        configureCntDiffColumn(
            parentCountDiff,
            TreeNodeDiff::getParentCountDiff,
            getText(ResourceUtil.COLUMN_PARENT_CNT));
    }

    @Override
    protected void refresh()
    {
        diff = new TreeProfileDiff();
        updateDiff(getBaseTarget(), true);
        updateDiff(getNewTarget(), false);
    }

    private void updateDiff(Profile profile, boolean base)
    {
        CopyAndFilterProfile task = new CopyAndFilterProfile(profile, getAdjustedProfileFilter());
        task.setOnSucceeded(state ->
        {
            // No need to worry about concurrency here, since this (the code for
            // onSucceeded()) will be executed on the FX thread. So even though
            // in the diff 2 tasks might execute concurrently during refresh(),
            // the resulting update calls in this if-statement won't execute
            // concurrently.
            if (base)
            {
                diff.updateForBase(task.getValue());
                diffTree.setRoot(new DiffTreeItem(diff));
                expandPartial(diffTree.getRoot(), 2);
            }
            else
            {
                diff.updateForNew(task.getValue());
                diffTree.setRoot(new DiffTreeItem(diff));
                expandPartial(diffTree.getRoot(), 2);
            }
        });
        appCtx().getExecutorService().execute(task);
    }

    private void configurePctColumn(TreeTableColumn<TreeNodeDiff, String> column,
        Function<TreeNodeDiff, Double> retriever, ProfileContext profileContext, String title)
    {
        column.setCellValueFactory(data -> wrapDouble(data, retriever));
        column.setCellFactory(col -> new TreeDiffViewCell());
        setColumnHeader(column, title, profileContext);
    }

    private void configurePctColumn(TreeTableColumn<TreeNodeDiff, String> column,
        Function<TreeNodeDiff, Double> retriever, String title)
    {
        column.setCellValueFactory(data -> wrapDouble(data, retriever));
        column.setCellFactory(col -> new TreeDiffViewCell());
        setColumnHeader(column, title, null);
    }

    private void configureCntColumn(TreeTableColumn<TreeNodeDiff, String> column,
        Function<TreeNodeDiff, Integer> retriever, ProfileContext profileContext, String title)
    {
        column.setCellValueFactory(data -> wrapInt(data, retriever));
        column.setCellFactory(col -> new TreeDiffViewCell());
        setColumnHeader(column, title, profileContext);
    }

    private void configureCntDiffColumn(TreeTableColumn<TreeNodeDiff, String> column,
        Function<TreeNodeDiff, Integer> retriever, String title)
    {
        column.setCellValueFactory(data -> wrapInt(data, retriever));
        column.setCellFactory(col -> new TreeDiffViewCell());
        setColumnHeader(column, title, null);
    }

    private void setColumnHeader(TreeTableColumn<?, ?> column, String title,
        ProfileContext profileContext)
    {
        HBox header = createColoredLabelContainer(CENTER);

        column.setText(null);
        column.setGraphic(header);

        if (profileContext != null)
        {
            addProfileNr(header, profileContext);
        }

        header.getChildren().add(new Text(title));

        // Somehow it's hard to get a TableColumn to resize properly.
        // Therefore, we calculate a fair width ourselves.
        double newWidth = calculateWidth(header);
        column.setMinWidth(newWidth);
        column.setPrefWidth(newWidth + 5); // some extra margin
    }

    private double calculateWidth(HBox box)
    {
        double width = 0;
        for (Node node : box.getChildren())
        {
            width += node.getBoundsInLocal().getWidth();
        }
        width += box.getSpacing() * (box.getChildren().size() - 1);
        width += box.getPadding().getLeft() + box.getPadding().getRight();
        return width;
    }

    private void applyQuickFilter()
    {
        String input = quickFilterText.getText();
        quickFilter = input.isEmpty() ? null : new StringFilter(
            Filter.Mode.CONTAINS,
            frame -> frame.getClassName() + "." + frame.getMethodName(),
            input);
        refresh();
    }

    private ProfileFilter getAdjustedProfileFilter()
    {
        if (quickFilter == null)
        {
            return currentFilter;
        }
        else
        {
            List<Filter> filters = new ArrayList<>();
            filters.add(quickFilter);
            filters.addAll(currentFilter.getFilters());
            return new ProfileFilter(filters);
        }
    }

    private StringProperty wrapDouble(CellDataFeatures<TreeNodeDiff, String> data,
        Function<TreeNodeDiff, Double> accessor)
    {
        return new ReadOnlyStringWrapper(
            data.getValue().getValue() != null
                && data.getValue().getValue() instanceof MethodNodeDiff
                    ? renderPercentage(accessor.apply(data.getValue().getValue())) : "");
    }

    private StringProperty wrapInt(CellDataFeatures<TreeNodeDiff, String> data,
        Function<TreeNodeDiff, Integer> accessor)
    {
        return new ReadOnlyStringWrapper(
            data.getValue().getValue() != null
                && data.getValue().getValue() instanceof MethodNodeDiff
                    ? Integer.toString(accessor.apply(data.getValue().getValue())) : "");
    }

    @Override
    protected void initializeInfoText()
    {
        info(filterButton, INFO_BUTTON_FILTER);
        info(expandAllButton, INFO_BUTTON_EXPANDALL);
        info(collapseAllButton, INFO_BUTTON_COLLAPSEALLALL);
        info(quickFilterText, INFO_INPUT_QUICKFILTER);
        info(quickFilterButton, INFO_BUTTON_QUICKFILTER);
        info(diffTree, INFO_TABLE_TREEDIFF);
    }

    private class DiffTreeItem extends TreeItem<TreeNodeDiff>
    {
        public DiffTreeItem(TreeProfileDiff profileDiff)
        {
            super(null);
            for (TreeNodeDiff child : profileDiff.getChildren())
            {
                getChildren().add(new DiffTreeItem(child));
            }
        }

        public DiffTreeItem(TreeNodeDiff diff)
        {
            super(diff);
            for (TreeNodeDiff child : diff.getChildren())
            {
                getChildren().add(new DiffTreeItem(child));
            }
        }
    }
}
