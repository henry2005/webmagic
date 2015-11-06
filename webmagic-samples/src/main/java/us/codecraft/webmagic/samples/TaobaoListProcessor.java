package us.codecraft.webmagic.samples;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.List;

/**
 * @author code4crafter@gmail.com <br>
 */
public class TaobaoListProcessor implements PageProcessor {

    public static final String URL_LIST = "https://handuyishe.tmall.com/search.htm?spm=\\w+&pageNo=\\w+";

    public static final String URL_POST = "http://blog\\.sina\\.com\\.cn/s/blog_\\w+\\.html";

    private Site site = Site
            .me()
            .setDomain("blog.sina.com.cn")
            .setSleepTime(3000)
            .setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

    @Override
    public void process(Page page) {

        String html = page.getRawText().replace("\\", "");
        //.replace("\"\"", "\"");

        Html h5 = new Html(html);

        //列表页
        List<String> urls = h5.links().regex(URL_LIST).all();


        Selectable selectable = h5.xpath("//dl[@class=\"item\"]");

        for(Selectable selectable1 : selectable.nodes()) {
//            System.out.println(selectable1.xpath("//dt[@class=photo]/a/img/@src"));
            System.out.println(selectable1.xpath("//dt[@class=photo]/a/@href"));
//            System.out.println(selectable1.xpath("//dt[@class=photo]/a/img/@data-ks-lazyload"));
            System.out.println(selectable1.xpath("//dd[@class=detail]/a[@class=item-name]/text()"));
            System.out.println(selectable1.xpath("//span[@class=c-price]/text()"));
        }

//        if (page.getUrl().regex(URL_LIST).match()) {
//            page.addTargetRequests(page.getHtml().xpath("//div[@class=\"articleList\"]").links().regex(URL_POST).all());
//            page.addTargetRequests(page.getHtml().links().regex(URL_LIST).all());
//            //文章页
//        } else {
//            page.putField("title", page.getHtml().xpath("//div[@class='articalTitle']/h2"));
//            page.putField("content", page.getHtml().xpath("//div[@id='articlebody']//div[@class='articalContent']"));
//            page.putField("date",
//                    page.getHtml().xpath("//div[@id='articlebody']//span[@class='time SG_txtc']").regex("\\((.*)\\)"));
//        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider spider = Spider.create(new TaobaoListProcessor())
               // .setDownloader(new SeleniumDownloader("/Users/quyan/Downloads/chromedriver"))
//                .addUrl("https://handuyishe.tmall.com/i/asynSearch.htm?_ksTS=1446031464228_122&callback=jsonp123&mid=w-1136113151-0&wid=1136113151&path=/search.htm&&search=y&spm=a1z10.3-b.w4011-1136113151.338.AX8mcY&pageNo=4&tsearch=y")
                .addUrl("https://shendacheng.tmall.com/i/asynSearch.htm?_ksTS=1446031464228_122&callback=jsonp123&mid=w-1136113151-0&wid=1136113151&path=/search.htm&&search=y&spm=a1z10.3-b.w4011-1136113151.338.AX8mcY&pageNo=1&tsearch=y");
        spider.run();
    }
}
