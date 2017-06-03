package com.blade.mvc.ui;

import com.blade.Blade;
import com.blade.metric.WebStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author biezhi
 *         2017/6/2
 */
public interface DefaultUI {

    String HTML_FOOTER = "<hr/><br/><p><center><a href='https://github.com/biezhi/blade' target='_blank'>Blade-" + Blade.VER + "</a></center></p>";
    
    /**
     * server 500
     */
    String VIEW_500 = new HtmlCreator().title("500 Internal Error").center("500 Internal Error").html();

    /**
     * server 404
     */
    String VIEW_404 = new HtmlCreator().title("404 Not Found").center("<h1>[ %s ] Not Found</h1>").html();

    String ERROR_START = new HtmlCreator().title("500 Internal Error").startStyle()
            .add("*{margin:0;padding:0;font-weight:400;}.info{margin:0;padding:10px;color:#000;background-color:#fff;height:60px;line-height:60px;border-bottom:5px solid #15557a}.isa_error{margin:0;padding:10px;font-size:14px;font-weight:bold;background-color:#e9eff1;border-bottom:1px solid #000}")
            .endStyle()
            .add("<div class='info'><h3>%s</h3></div><div class='isa_error'><pre>").toString();

    WebStatistics WEB_STATISTICS = WebStatistics.me();

    Locale locale = Locale.getDefault().getLanguage().equals("zh") ? Locale.CHINESE : Locale.ENGLISH;
    ResourceBundle bundle = ResourceBundle.getBundle("i18n", locale);

    enum MonitorEnum {
        statistics, total_requests, unique_requests,
        open_connections, requests, ip, no_completed_requests,
        redirects, no_redirects, connections, established, closed,
        sent, received, speed
    }


    public static String getKey(MonitorEnum monitorEnum) {
        return bundle.getString(monitorEnum.name());
    }

    public static void registerStatus(Blade blade) {
        blade.get("/blade/monitor", ((request, response) -> {
            HtmlCreator htmlCreator = new HtmlCreator();
            htmlCreator.title(getKey(MonitorEnum.statistics));
            htmlCreator.h1(getKey(MonitorEnum.statistics));

            htmlCreator.startP().addBold(getKey(MonitorEnum.total_requests))
                    .add("" + WEB_STATISTICS.getNumberOfRequests()).endP();

            htmlCreator.startP().addBold(getKey(MonitorEnum.unique_requests))
                    .add("" + WEB_STATISTICS.getNumberOfUniqueRequests()).endP();

            htmlCreator.startP().addBold(getKey(MonitorEnum.open_connections))
                    .add("" + WEB_STATISTICS.getConnectionCount()).endP();

            htmlCreator.hr();
            htmlCreator.h2(getKey(MonitorEnum.requests));
            if (WEB_STATISTICS.getIpRequestsAsStrings().size() == 0) {
                htmlCreator.paragraph(getKey(MonitorEnum.no_completed_requests));
            } else {
                List<String> requestsTableHeaders = Arrays.asList(getKey(MonitorEnum.ip), getKey(MonitorEnum.requests), "Date and time of last request");
                htmlCreator.addTableWithHeaders(requestsTableHeaders);
                WEB_STATISTICS.getIpRequestsAsStrings().forEach(htmlCreator::addRowToTable);
                htmlCreator.endTable();
            }

            htmlCreator.hr();
            htmlCreator.h2(getKey(MonitorEnum.redirects));
            if (WEB_STATISTICS.getRedirectsAsStrings().size() == 0) {
                htmlCreator.paragraph(getKey(MonitorEnum.no_redirects));
            } else {
                List<String> redirectsTableHeaders = Arrays.asList("Destination URL", "Number of redirects");
                htmlCreator.addTableWithHeaders(redirectsTableHeaders);
                WEB_STATISTICS.getRedirectsAsStrings().forEach(htmlCreator::addRowToTable);
                htmlCreator.endTable();
            }

            htmlCreator.hr();
            htmlCreator.h2(getKey(MonitorEnum.connections));

            List<String> connectionsTableHeaders = Arrays.asList(getKey(MonitorEnum.ip),
                    "URIs", getKey(MonitorEnum.established), getKey(MonitorEnum.closed),
                    getKey(MonitorEnum.sent), getKey(MonitorEnum.received), getKey(MonitorEnum.speed));

            htmlCreator.addTableWithHeaders(connectionsTableHeaders);
            WEB_STATISTICS.getConnectionsAsStrings().forEach(htmlCreator::addRowToTable);
            htmlCreator.endTable();

            htmlCreator.startStyle().centerHeadings().styleTables().endStyle().br().br().br();

            response.html(htmlCreator.html());
        }));
    }

}
