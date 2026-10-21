package com.example.admin.module.user.support;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.util.List;
import java.util.Map;

/**
 * 导入模板下拉验证处理器。
 * 在指定列添加 Excel 数据验证下拉框，限制用户只能选择预设选项。
 * 列索引对应 UserImportRow 字段顺序（0 起）：部门=2、状态=5、角色=6。
 */
public class DropdownValidationHandler implements SheetWriteHandler {

    /** 列索引 → 下拉选项列表 */
    private final Map<Integer, List<String>> dropdowns;

    /** 验证覆盖的最大行号（0 为 header，数据从 1 开始；留足余量给用户追加） */
    private final int lastRow;

    public DropdownValidationHandler(Map<Integer, List<String>> dropdowns, int lastRow) {
        this.dropdowns = dropdowns;
        this.lastRow = lastRow;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();

        dropdowns.forEach((colIndex, options) -> {
            String[] optionArray = options.toArray(new String[0]);
            DataValidationConstraint constraint = dvHelper.createExplicitListConstraint(optionArray);
            // 第 1 行到 lastRow 行，指定列单独校验
            CellRangeAddressList range = new CellRangeAddressList(1, lastRow, colIndex, colIndex);
            DataValidation validation = dvHelper.createValidation(constraint, range);
            sheet.addValidationData(validation);
        });
    }
}
