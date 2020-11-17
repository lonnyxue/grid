package org.xiaochao.tool;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.setting.Setting;
import cn.hutool.setting.SettingUtil;
import org.xiaochao.model.GridModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DEGridV2 {
    private static final String GENERATE_FILE_DIR;
    private static final String FILE_NAME;
    private static final double PER_GRID;
    private static final double MAX_LOSS;
    private static final int BUY_SELL_NUM;
    private static final double CURRENT_PRICE;
    private static final double MAX_PRICE;

    private static final double BUY_AMOUNT;

    private static  final  int  retention_digit;

    static {
        Setting setting = SettingUtil.get("grid_init.properties");
        GENERATE_FILE_DIR = setting.getStr("generate_file_dir");
        FILE_NAME = setting.getStr("file_name");
        PER_GRID = setting.getDouble("per_grid");
        MAX_LOSS = setting.getDouble("max_loss");
        CURRENT_PRICE = setting.getDouble("current_price");
        MAX_PRICE = setting.getDouble("max_price");
        BUY_SELL_NUM = setting.getInt("buy_sell_num");
        BUY_AMOUNT = setting.getDouble("buy_amount");
        retention_digit = setting.getInt("retention_digit");
    }

    public static void main(String[] args) {
        DEGridV2 grid = new DEGridV2();
        grid.grid();

    }


    public void grid() {
        List<GridModel> gridModels = gen();
        write2Excel(gridModels);
    }


    private List<GridModel> gen() {
        List<GridModel> gridModels = genPricesLte();
//        gridModels.addAll(genPricesLte());
        gridModels.add(genSum(gridModels));
        return gridModels;

    }

    private List<GridModel> genPricesGt() {
        List<GridModel> gridModels = new ArrayList<>();
        double nextBuyPrice;
        int level = 1;
        do {
            double gridBuyLevel = 1.0 + PER_GRID * level / 100;
            double gridSellLevel = 1.0 + PER_GRID * (level + 1) / 100;
            nextBuyPrice = CURRENT_PRICE * gridBuyLevel;
            double nextSellPrice = CURRENT_PRICE * gridSellLevel;
            level++;
            gridModels.add(createOneGrid(nextBuyPrice, nextSellPrice, gridBuyLevel));
        } while (nextBuyPrice < MAX_PRICE);
        gridModels.sort(Comparator.comparingDouble(GridModel::getLevel).reversed());
        return gridModels;
    }

    private List<GridModel> genPricesLte() {
        List<GridModel> gridModels = new ArrayList<>();
        int grids = (int) (MAX_LOSS / PER_GRID);
        for (int i = 0; i <= grids; i++) {
            double gridBuyLevel = 1.0 - PER_GRID * i / 100;
            double gridSellLevel = 1.0 - PER_GRID * (i - 1) / 100;
            double buyPrice = CURRENT_PRICE * gridBuyLevel;
            double sellPrice = CURRENT_PRICE * gridSellLevel;
            gridModels.add(createOneGrid(buyPrice, sellPrice, gridBuyLevel));
        }
        return gridModels;
    }

    private GridModel genSum(List<GridModel> gridModels) {
        GridModel gridModel = new GridModel();
        gridModel.setBuyNum(gridModels.stream().mapToInt(GridModel::getBuyNum).sum());
        gridModel.setBuyPriceSum(gridModels.stream().mapToDouble(GridModel::getBuyPriceSum).sum());
        gridModel.setSellNum(gridModels.stream().mapToInt(GridModel::getSellNum).sum());
        gridModel.setSellPriceSum(gridModels.stream().mapToDouble(GridModel::getSellPriceSum).sum());
        gridModel.setProfit(gridModels.stream().mapToDouble(GridModel::getProfit).sum());
        gridModel.setProfitPercentage(gridModel.getProfit() / gridModel.getBuyPriceSum() * 100);
        return gridModel;
    }

    private GridModel createOneGrid(double buyPrice, double sellPrice, double buyLevel) {
        GridModel gridModel = new GridModel();
        gridModel.setLevel(buyLevel);
        gridModel.setBuyPrice(buyPrice);
        int buyNum = (int) Math.round(BUY_AMOUNT / buyPrice);
        gridModel.setBuyNum(buyNum);
        gridModel.setBuyPriceSum(buyPrice * gridModel.getBuyNum());

        gridModel.setSellPrice(sellPrice);
        //原卖出数量
        int oldSellNum=(int) Math.round(BUY_AMOUNT / sellPrice);
        //留存量=（原卖出数量-原卖出数量）* 留存收益份数
        int LeftNum= (buyNum - oldSellNum)*retention_digit;
        gridModel.setLeftNum(LeftNum);
        //本次卖出数量=原卖出数量-留存量
        gridModel.setSellNum(oldSellNum - ((buyNum - oldSellNum) * (retention_digit - 1)));
        gridModel.setSellPriceSum((gridModel.getSellPrice() * gridModel.getSellNum()));
        gridModel.setProfit(gridModel.getLeftNum() * sellPrice);
        gridModel.setProfitPercentage(gridModel.getProfit() / gridModel.getBuyPriceSum() * 100);
        return gridModel;
    }

    private void write2Excel(List<GridModel> gridModels) {
        String path=GENERATE_FILE_DIR + File.separator + FILE_NAME + System.currentTimeMillis() + "1.0" + ".xlsx";
        System.out.println(path);
        ExcelWriter writer = ExcelUtil.getWriter(path);
        writer.addHeaderAlias("level", "与基准比较");
        writer.addHeaderAlias("buyPrice", "买入价格");
        writer.addHeaderAlias("buyNum", "买入数量");
        writer.addHeaderAlias("buyPriceSum", "买入价格合计");
        writer.addHeaderAlias("sellPrice", "卖出价格");
        writer.addHeaderAlias("sellNum", "卖出数量");
        writer.addHeaderAlias("sellPriceSum", "卖出价格合计");
        writer.addHeaderAlias("leftNum", "留存量");
        writer.addHeaderAlias("profit", "盈利");
        writer.addHeaderAlias("profitPercentage", "盈利百分比");
        writer.write(gridModels);
        writer.setColumnWidth(0, 20);
        writer.setColumnWidth(1, 20);
        writer.setColumnWidth(2, 20);
        writer.setColumnWidth(3, 20);
        writer.setColumnWidth(4, 20);
        writer.setColumnWidth(5, 20);
        writer.setColumnWidth(6, 20);
        writer.setColumnWidth(7, 20);
        writer.setColumnWidth(8, 20);
        writer.setColumnWidth(9, 20);
        writer.flush();
        writer.close();
    }
}
