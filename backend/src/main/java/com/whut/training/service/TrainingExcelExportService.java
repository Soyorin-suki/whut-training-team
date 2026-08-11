package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.TrainingExportRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.MemberType;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.CodeforcesRatingHistoryRepository;
import com.whut.training.repository.AtCoderTrackingRepository;
import com.whut.training.repository.TrainingDashboardRepository;
import com.whut.training.repository.UserRepository;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生成现役队员训练数据的多工作表 Excel 文件。
 */
@Service
public class TrainingExcelExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final TrainingDashboardRepository trainingDashboardRepository;
    private final CodeforcesRatingHistoryRepository ratingHistoryRepository;
    private final CodeforcesProfileService codeforcesProfileService;
    private final AtCoderTrackingRepository atCoderTrackingRepository;
    private final TimeProvider timeProvider;

    public TrainingExcelExportService(
            UserRepository userRepository,
            TrainingDashboardRepository trainingDashboardRepository,
            CodeforcesRatingHistoryRepository ratingHistoryRepository,
            CodeforcesProfileService codeforcesProfileService,
            AtCoderTrackingRepository atCoderTrackingRepository,
            TimeProvider timeProvider
    ) {
        this.userRepository = userRepository;
        this.trainingDashboardRepository = trainingDashboardRepository;
        this.ratingHistoryRepository = ratingHistoryRepository;
        this.codeforcesProfileService = codeforcesProfileService;
        this.atCoderTrackingRepository = atCoderTrackingRepository;
        this.timeProvider = timeProvider;
    }

    public ExportResult export(TrainingExportRequest request) {
        if (request == null || (!request.includeDaily()
                && !request.includeCodeforcesContests()
                && !request.includeAtCoderContests())) {
            throw new BusinessException(400, "at least one export data type is required");
        }

        LocalDate today = timeProvider.today();
        LocalDate startDate = switch (request.exportRange()) {
            case WEEK -> today.minusDays(6);
            case MONTH -> today.minusDays(29);
            case ALL -> null;
        };
        Long startTimeSeconds = startDate == null
                ? null
                : startDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toEpochSecond();

        List<User> members = userRepository.findByMemberType(MemberType.ACTIVE_TEAM);
        if (request.includeCodeforcesContests()) {
            members.stream()
                    .filter(member -> member.getCodeforcesHandle() != null
                            && !member.getCodeforcesHandle().isBlank())
                    .forEach(member -> codeforcesProfileService.ensureRatingHistory(member.getId()));
        }
        List<TrainingDashboardRepository.DailyExportRow> dailyRows = request.includeDaily()
                ? trainingDashboardRepository.findActiveTeamDailyExport(startDate)
                : List.of();
        List<CodeforcesRatingHistoryRepository.RatingContestExportRow> contestRows =
                request.includeCodeforcesContests()
                        ? ratingHistoryRepository.findActiveTeamHistory(startTimeSeconds)
                        : List.of();
        List<AtCoderTrackingRepository.AtCoderExportRow> atCoderRows = request.includeAtCoderContests()
                ? atCoderTrackingRepository.findExportRows(startTimeSeconds)
                : List.of();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeSummarySheet(workbook, styles, members, dailyRows, contestRows, request, startDate, today);
            if (request.includeDaily()) writeDailySheet(workbook, styles, dailyRows, startDate, today);
            if (request.includeCodeforcesContests()) writeContestSheet(workbook, styles, contestRows, startDate, today);
            if (request.includeAtCoderContests()) writeAtCoderSheet(workbook, styles, atCoderRows, startDate, today);
            workbook.setActiveSheet(0);
            workbook.write(output);
            String rangeName = switch (request.exportRange()) {
                case WEEK -> "最近一周";
                case MONTH -> "最近一个月";
                case ALL -> "全部";
            };
            String filename = "WHUT-ACM_现役队员训练数据_" + rangeName + "_" + FILE_DATE.format(today) + ".xlsx";
            return new ExportResult(filename, output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("failed to generate training workbook", ex);
        }
    }

    private void writeSummarySheet(
            XSSFWorkbook workbook,
            Styles styles,
            List<User> members,
            List<TrainingDashboardRepository.DailyExportRow> dailyRows,
            List<CodeforcesRatingHistoryRepository.RatingContestExportRow> contestRows,
            TrainingExportRequest request,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet("成员汇总");
        sheet.setDisplayGridlines(false);
        String[] headers = {
                "站内账号", "成员姓名", "Codeforces账号", "每日题题目数", "每日题完成数",
                "每日题完成率", "每日题活跃天数", "CF Rated比赛数", "Rating总变化",
                "当前Rating", "数据说明"
        };
        createTitle(sheet, styles, headers.length, "WHUT-ACM 现役队员训练数据汇总");
        Row meta = sheet.createRow(1);
        textCell(meta, 0, "导出范围：" + rangeLabel(startDate, endDate), styles.meta);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));
        Row note = sheet.createRow(2);
        textCell(note, 0, "导出内容：" + selectedTypesLabel(request), styles.meta);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, headers.length - 1));
        int headerRowIndex = 4;
        createHeader(sheet, headerRowIndex, headers, styles);

        Map<Long, DailyStats> dailyStats = aggregateDaily(dailyRows);
        Map<Long, ContestStats> contestStats = aggregateContests(contestRows);

        int rowIndex = headerRowIndex + 1;
        for (User member : members) {
            Row row = sheet.createRow(rowIndex++);
            CellStyle base = row.getRowNum() % 2 == 0 ? styles.bodyAlternate : styles.body;
            DailyStats daily = dailyStats.getOrDefault(member.getId(), new DailyStats());
            ContestStats contests = contestStats.getOrDefault(member.getId(), new ContestStats());
            textCell(row, 0, member.getUsername(), base);
            textCell(row, 1, displayName(member), base);
            textCell(row, 2, member.getCodeforcesHandle(), base);
            nullableNumberCell(row, 3, request.includeDaily() ? daily.assigned : null, styles.integer);
            nullableNumberCell(row, 4, request.includeDaily() ? daily.completed : null, styles.integer);
            if (request.includeDaily()) {
                numberCell(row, 5, daily.assigned == 0 ? 0 : daily.completed * 1.0 / daily.assigned, styles.percent);
                numberCell(row, 6, daily.activeDates.size(), styles.integer);
            } else {
                textCell(row, 5, null, base);
                textCell(row, 6, null, base);
            }
            nullableNumberCell(row, 7, request.includeCodeforcesContests() ? contests.count : null, styles.integer);
            nullableNumberCell(row, 8, request.includeCodeforcesContests() ? contests.ratingDelta : null, styles.integer);
            nullableNumberCell(row, 9, request.includeCodeforcesContests()
                    ? (contests.latestRating == null ? member.getCodeforcesRating() : contests.latestRating)
                    : null, styles.integer);
            textCell(row, 10, buildMemberNote(request, member, contests), base);
        }
        finishTable(sheet, headerRowIndex, rowIndex - 1, headers.length,
                new int[]{16, 18, 20, 15, 15, 16, 16, 18, 16, 15, 32});
    }

    private void writeDailySheet(
            XSSFWorkbook workbook,
            Styles styles,
            List<TrainingDashboardRepository.DailyExportRow> rows,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet("每日一题");
        sheet.setDisplayGridlines(false);
        String[] headers = {
                "日期", "站内账号", "成员姓名", "题目类型", "题目编号", "题目名称",
                "Rating", "标签", "完成状态", "提交ID", "判题结果", "获得积分", "题目链接"
        };
        createTitle(sheet, styles, headers.length, "每日一题完成明细");
        createRangeMeta(sheet, styles, headers.length, startDate, endDate);
        int headerRowIndex = 3;
        createHeader(sheet, headerRowIndex, headers, styles);
        int rowIndex = headerRowIndex + 1;
        for (TrainingDashboardRepository.DailyExportRow item : rows) {
            Row row = sheet.createRow(rowIndex++);
            CellStyle base = row.getRowNum() % 2 == 0 ? styles.bodyAlternate : styles.body;
            dateCell(row, 0, item.date(), styles.date);
            textCell(row, 1, item.username(), base);
            textCell(row, 2, item.displayName(), base);
            textCell(row, 3, slotLabel(item.slot()), base);
            textCell(row, 4, item.problemKey(), base);
            textCell(row, 5, item.problemName(), base);
            nullableNumberCell(row, 6, item.rating(), styles.integer);
            textCell(row, 7, item.tags(), base);
            textCell(row, 8, item.completed() ? "已完成" : "未完成",
                    item.completed() ? styles.completed : styles.pending);
            textCell(row, 9, item.submissionId() == null ? null : String.valueOf(item.submissionId()), base);
            textCell(row, 10, item.verdict(), base);
            nullableNumberCell(row, 11, item.score(), styles.integer);
            hyperlinkCell(workbook, row, 12, item.sourceUrl(), styles.hyperlink);
        }
        finishTable(sheet, headerRowIndex, rowIndex - 1, headers.length,
                new int[]{13, 16, 18, 13, 15, 32, 11, 30, 13, 16, 14, 13, 38});
    }

    private void writeContestSheet(
            XSSFWorkbook workbook,
            Styles styles,
            List<CodeforcesRatingHistoryRepository.RatingContestExportRow> rows,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet("CF Rating比赛");
        sheet.setDisplayGridlines(false);
        String[] headers = {
                "比赛时间", "站内账号", "成员姓名", "CF账号", "比赛ID", "比赛名称",
                "排名", "赛前Rating", "赛后Rating", "Rating变化", "比赛链接"
        };
        createTitle(sheet, styles, headers.length, "Codeforces Rated 比赛完成明细");
        createRangeMeta(sheet, styles, headers.length, startDate, endDate);
        int headerRowIndex = 3;
        createHeader(sheet, headerRowIndex, headers, styles);
        int rowIndex = headerRowIndex + 1;
        for (CodeforcesRatingHistoryRepository.RatingContestExportRow item : rows) {
            Row row = sheet.createRow(rowIndex++);
            CellStyle base = row.getRowNum() % 2 == 0 ? styles.bodyAlternate : styles.body;
            dateTimeCell(row, 0, item.ratingUpdateTimeSeconds(), styles.dateTime);
            textCell(row, 1, item.username(), base);
            textCell(row, 2, item.displayName(), base);
            textCell(row, 3, item.codeforcesHandle(), base);
            textCell(row, 4, item.contestId() == null ? null : String.valueOf(item.contestId()), base);
            textCell(row, 5, item.contestName(), base);
            nullableNumberCell(row, 6, item.rank(), styles.integer);
            nullableNumberCell(row, 7, item.oldRating(), styles.integer);
            nullableNumberCell(row, 8, item.newRating(), styles.integer);
            CellStyle changeStyle = item.ratingChange() == null
                    ? styles.integer
                    : item.ratingChange() >= 0 ? styles.positive : styles.negative;
            nullableNumberCell(row, 9, item.ratingChange(), changeStyle);
            hyperlinkCell(workbook, row, 10, item.contestUrl(), styles.hyperlink);
        }
        finishTable(sheet, headerRowIndex, rowIndex - 1, headers.length,
                new int[]{20, 16, 18, 20, 13, 38, 12, 14, 14, 14, 38});
    }

    private void writeAtCoderSheet(
            XSSFWorkbook workbook,
            Styles styles,
            List<AtCoderTrackingRepository.AtCoderExportRow> rows,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet("AtCoder ABC");
        sheet.setDisplayGridlines(false);
        String[] headers = {
                "比赛时间", "比赛ID", "比赛名称", "站内账号", "成员姓名", "AtCoder账号",
                "检查状态", "是否参赛", "AC数", "通过题目", "比赛排名", "Performance",
                "赛前Rating", "赛后Rating", "豁免原因", "最近检查", "比赛链接"
        };
        createTitle(sheet, styles, headers.length, "AtCoder Beginner Contest 周赛完成明细");
        createRangeMeta(sheet, styles, headers.length, startDate, endDate);
        int headerRowIndex = 3;
        createHeader(sheet, headerRowIndex, headers, styles);
        int rowIndex = headerRowIndex + 1;
        for (AtCoderTrackingRepository.AtCoderExportRow item : rows) {
            Row row = sheet.createRow(rowIndex++);
            CellStyle base = row.getRowNum() % 2 == 0 ? styles.bodyAlternate : styles.body;
            dateTimeCell(row, 0, item.startTimeSeconds(), styles.dateTime);
            textCell(row, 1, item.contestId(), base);
            textCell(row, 2, item.contestName(), base);
            textCell(row, 3, item.username(), base);
            textCell(row, 4, item.displayName(), base);
            textCell(row, 5, item.atcoderHandle(), base);
            String status = item.exempted() ? "EXEMPT" : item.status();
            textCell(row, 6, atCoderStatusLabel(status),
                    "COMPLETED".equals(status) ? styles.completed : styles.pending);
            textCell(row, 7, item.participated() ? "是" : "否", base);
            nullableNumberCell(row, 8, item.acCount(), styles.integer);
            textCell(row, 9, item.solvedProblemIds(), base);
            nullableNumberCell(row, 10, item.contestRank(), styles.integer);
            nullableNumberCell(row, 11, item.performance(), styles.integer);
            nullableNumberCell(row, 12, item.oldRating(), styles.integer);
            nullableNumberCell(row, 13, item.newRating(), styles.integer);
            textCell(row, 14, item.exemptionReason(), base);
            textCell(row, 15, item.checkedAt(), base);
            hyperlinkCell(workbook, row, 16, item.contestUrl(), styles.hyperlink);
        }
        finishTable(sheet, headerRowIndex, rowIndex - 1, headers.length,
                new int[]{20, 13, 38, 16, 18, 20, 16, 12, 10, 30, 12, 15, 14, 14, 28, 24, 38});
    }

    private Map<Long, DailyStats> aggregateDaily(List<TrainingDashboardRepository.DailyExportRow> rows) {
        Map<Long, DailyStats> result = new HashMap<>();
        for (TrainingDashboardRepository.DailyExportRow row : rows) {
            DailyStats stats = result.computeIfAbsent(row.userId(), ignored -> new DailyStats());
            stats.assigned += 1;
            if (row.completed()) {
                stats.completed += 1;
                stats.activeDates.add(row.date());
            }
        }
        return result;
    }

    private Map<Long, ContestStats> aggregateContests(
            List<CodeforcesRatingHistoryRepository.RatingContestExportRow> rows
    ) {
        Map<Long, ContestStats> result = new HashMap<>();
        for (CodeforcesRatingHistoryRepository.RatingContestExportRow row : rows) {
            ContestStats stats = result.computeIfAbsent(row.userId(), ignored -> new ContestStats());
            stats.count += 1;
            stats.ratingDelta += row.ratingChange() == null ? 0 : row.ratingChange();
            if (stats.latestTime == null || (row.ratingUpdateTimeSeconds() != null
                    && row.ratingUpdateTimeSeconds() > stats.latestTime)) {
                stats.latestTime = row.ratingUpdateTimeSeconds();
                stats.latestRating = row.newRating();
            }
        }
        return result;
    }

    private void createTitle(Sheet sheet, Styles styles, int columnCount, String title) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30);
        textCell(row, 0, title, styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));
    }

    private void createRangeMeta(Sheet sheet, Styles styles, int columnCount, LocalDate startDate, LocalDate endDate) {
        Row meta = sheet.createRow(1);
        textCell(meta, 0, "数据范围：" + rangeLabel(startDate, endDate), styles.meta);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));
    }

    private void createHeader(Sheet sheet, int rowIndex, String[] headers, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24);
        for (int column = 0; column < headers.length; column += 1) {
            textCell(row, column, headers[column], styles.header);
        }
    }

    private void finishTable(Sheet sheet, int headerRow, int lastRow, int columnCount, int[] widths) {
        sheet.createFreezePane(0, headerRow + 1);
        if (lastRow >= headerRow) {
            sheet.setAutoFilter(new CellRangeAddress(headerRow, Math.max(headerRow, lastRow), 0, columnCount - 1));
        }
        for (int column = 0; column < widths.length; column += 1) {
            sheet.setColumnWidth(column, Math.min(60, widths[column]) * 256);
        }
    }

    private void textCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void numberCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void nullableNumberCell(Row row, int column, Number value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private void dateCell(Row row, int column, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(Date.valueOf(value));
        cell.setCellStyle(style);
    }

    private void dateTimeCell(Row row, int column, Long epochSeconds, CellStyle style) {
        Cell cell = row.createCell(column);
        if (epochSeconds != null) cell.setCellValue(new java.util.Date(epochSeconds * 1000));
        cell.setCellStyle(style);
    }

    private void hyperlinkCell(XSSFWorkbook workbook, Row row, int column, String url, CellStyle style) {
        Cell cell = row.createCell(column);
        if (url != null && !url.isBlank()) {
            cell.setCellValue(url);
            var hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(url);
            cell.setHyperlink(hyperlink);
        }
        cell.setCellStyle(style);
    }

    private String selectedTypesLabel(TrainingExportRequest request) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        if (request.includeDaily()) names.add("每日一题");
        if (request.includeCodeforcesContests()) names.add("CF Rating 比赛");
        if (request.includeAtCoderContests()) names.add("AtCoder ABC");
        return String.join("、", names);
    }

    private String atCoderStatusLabel(String status) {
        if (status == null) return "待同步";
        return switch (status) {
            case "COMPLETED" -> "已完成";
            case "PARTICIPATED" -> "已参赛未达标";
            case "ABSENT" -> "缺席";
            case "UNBOUND" -> "未绑定";
            case "EXEMPT" -> "已豁免";
            case "DATA_ERROR" -> "数据异常";
            case "UPCOMING" -> "待比赛";
            default -> "等待同步";
        };
    }

    private String rangeLabel(LocalDate startDate, LocalDate endDate) {
        return startDate == null ? "全部历史数据（截至 " + endDate + "）" : startDate + " 至 " + endDate;
    }

    private String buildMemberNote(
            TrainingExportRequest request,
            User member,
            ContestStats contestStats
    ) {
        if (!request.includeCodeforcesContests()) return "";
        if (member.getCodeforcesHandle() == null || member.getCodeforcesHandle().isBlank()) return "未绑定 Codeforces";
        if (contestStats.count == 0) return "选定范围内无已同步 Rated 比赛";
        return "CF 比赛数据来自平台本地同步记录";
    }

    private String displayName(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }

    private String slotLabel(String slot) {
        if ("easy".equalsIgnoreCase(slot)) return "简单题";
        if ("hard".equalsIgnoreCase(slot)) return "困难题";
        return "每日题";
    }

    public record ExportResult(String filename, byte[] content) {
    }

    private static final class DailyStats {
        private int assigned;
        private int completed;
        private final Set<LocalDate> activeDates = new HashSet<>();
    }

    private static final class ContestStats {
        private int count;
        private int ratingDelta;
        private Long latestTime;
        private Integer latestRating;
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle meta;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle bodyAlternate;
        private final CellStyle integer;
        private final CellStyle percent;
        private final CellStyle date;
        private final CellStyle dateTime;
        private final CellStyle hyperlink;
        private final CellStyle completed;
        private final CellStyle pending;
        private final CellStyle positive;
        private final CellStyle negative;

        private Styles(XSSFWorkbook workbook) {
            title = workbook.createCellStyle();
            setFillColor(title, "132238");
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.LEFT);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title.setFont(titleFont);

            meta = workbook.createCellStyle();
            setFillColor(meta, "E8EEF5");
            meta.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            meta.setVerticalAlignment(VerticalAlignment.CENTER);
            Font metaFont = workbook.createFont();
            setFontColor(metaFont, "44546A");
            metaFont.setFontHeightInPoints((short) 10);
            meta.setFont(metaFont);

            header = workbook.createCellStyle();
            setFillColor(header, "183651");
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBottomBorderColor(IndexedColors.BLUE_GREY.getIndex());
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(headerFont);

            body = bodyStyle(workbook, "FFFFFF");
            bodyAlternate = bodyStyle(workbook, "F6F8FA");
            integer = workbook.createCellStyle();
            integer.cloneStyleFrom(body);
            integer.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            integer.setAlignment(HorizontalAlignment.RIGHT);
            percent = workbook.createCellStyle();
            percent.cloneStyleFrom(body);
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
            percent.setAlignment(HorizontalAlignment.RIGHT);
            date = workbook.createCellStyle();
            date.cloneStyleFrom(body);
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            dateTime = workbook.createCellStyle();
            dateTime.cloneStyleFrom(body);
            dateTime.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
            hyperlink = workbook.createCellStyle();
            hyperlink.cloneStyleFrom(body);
            Font linkFont = workbook.createFont();
            setFontColor(linkFont, "0563C1");
            linkFont.setUnderline(Font.U_SINGLE);
            hyperlink.setFont(linkFont);

            completed = statusStyle(workbook, "EAF8ED", "216E39");
            pending = statusStyle(workbook, "FDECEC", "B42318");
            positive = statusStyle(workbook, "EAF8ED", "216E39");
            positive.setDataFormat(workbook.createDataFormat().getFormat("+0;-0;0"));
            negative = statusStyle(workbook, "FDECEC", "B42318");
            negative.setDataFormat(workbook.createDataFormat().getFormat("+0;-0;0"));
        }

        private static CellStyle bodyStyle(XSSFWorkbook workbook, String fill) {
            CellStyle style = workbook.createCellStyle();
            setFillColor(style, fill);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.HAIR);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            Font font = workbook.createFont();
            setFontColor(font, "132238");
            font.setFontHeightInPoints((short) 10);
            style.setFont(font);
            return style;
        }

        private static CellStyle statusStyle(XSSFWorkbook workbook, String fill, String fontColor) {
            CellStyle style = bodyStyle(workbook, fill);
            style.setAlignment(HorizontalAlignment.CENTER);
            Font font = workbook.createFont();
            font.setBold(true);
            setFontColor(font, fontColor);
            style.setFont(font);
            return style;
        }

        private static XSSFColor color(String hex) {
            return new XSSFColor(java.awt.Color.decode("#" + hex), null);
        }

        private static void setFillColor(CellStyle style, String hex) {
            ((XSSFCellStyle) style).setFillForegroundColor(color(hex));
        }

        private static void setFontColor(Font font, String hex) {
            ((XSSFFont) font).setColor(color(hex));
        }
    }
}
