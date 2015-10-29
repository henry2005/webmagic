package us.codecraft.webmagic.samples;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

/**
 * @author code4crafter@gmail.com <br>
 */
public class TaobaoProcessor implements PageProcessor {

    public static final String URL_LIST = "https://handuyishe.tmall.com/search.htm?spm=\\w+&pageNo=\\w+";

    private Site site = Site
            .me()
            .setDomain("blog.sina.com.cn")
            .setSleepTime(3000);

    @Override
    public void process(Page page) {
        //列表页
//        if (page.getUrl().regex(URL_LIST).match()) {
            //文章页
//        } else {
        Selectable selectable = page.getHtml().xpath("//dl[@class=\"item\"]");

        for(Selectable selectable1 : selectable.nodes()) {
            System.out.println(selectable1.xpath("//dt[@class=photo]/a/img/@src"));
            System.out.println(selectable1.xpath("//dt[@class=photo]/a/img/@data-ks-lazyload"));
            System.out.println(selectable1.xpath("//dd[@class=detail]/a[@class=item-name]/text()"));
            System.out.println(selectable1.xpath("//span[@class=c-price]/text()"));
        }


        Selectable selectable2 = page.getHtml().regex(URL_LIST);


//        page.addTargetRequest();

//        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(
                new TaobaoProcessor())
                .setDownloader(new SeleniumDownloader("/Users/quyan/Downloads/chromedriver"))
                .addUrl("https://handuyishe.tmall.com/search.htm?spm=a1z10.5-b.w4011-1136113148.1.nj3WDr")
                .run();
    }
}
