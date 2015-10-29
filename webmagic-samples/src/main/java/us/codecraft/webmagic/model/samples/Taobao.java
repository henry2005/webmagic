package us.codecraft.webmagic.model.samples;

import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.model.ConsolePageModelPipeline;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.model.annotation.TargetUrl;
import us.codecraft.webmagic.pipeline.PageModelPipeline;

/**
 * @author code4crafter@gmail.com
 * @date 14-4-11
 */
@TargetUrl("https://handuyishe.tmall.com/search.htm?spm=\\w+")
@ExtractBy(value = "//dl[@class=item]",multi = true)
public class Taobao {

    @ExtractBy("//dt[@class=photo]/a/img/@src")
    private String shopName;

    @ExtractBy("//dd[@class=detail]/a[@class=item-name]/text()")
    private String promo;

    public static void main(String[] args) {
        OOSpider.create(Site.me(), new PageModelPipeline() {
            @Override
            public void process(Object o, Task task) {
                System.out.println(((Taobao) o).shopName + "      " + ((Taobao) o).promo);
            }
        }, Taobao.class)
                .setDownloader(new SeleniumDownloader("/Users/quyan/Downloads/chromedriver"))
                .addUrl("https://handuyishe.tmall.com/search.htm?spm=a1z10.5-b.w4011-1136113148.1.nj3WDr").thread(4).run();
    }

}
