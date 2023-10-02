package me.chyxion.tigon.test;

import lombok.val;
import org.junit.Test;
import org.junit.Assert;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import me.chyxion.tigon.mybatis.Search;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Donghuang
 * @date May 13, 2016 10:45:07 AM
 */
@Slf4j
public class TestDriver {

    @Test
    public void runSearch() {
        Search search = new Search(1).eq("name", 2).or("gender", "F").or("gender", "M");
        log.info(StringUtils.join(search.assemble(), ""));
        search = new Search().or(
                new Search().or("Bar", "Foo")
                        .eq("foo", "bar")
                        .ne("ne_col", "1")
                        .ne("not_null", null)
                        .notNull("not_null2")
                        .or(new Search().eq("sub_or0", 1).eq("sub_or2", 2))
                        .in("in_col", new Object[] {"1", 2, 3})
                        .like("col_like", "%Shaun%")
                        .or("Or0", "OrValue")
                        .or("Or2", "OrValue2")
            ).eq("name", 2).isNull("gaga");
        log.info(StringUtils.join(search.assemble(), ""));
    }

    @Test
    public void runArray() {
        Object array = new String[] {"1", "2"};
        System.err.println(Arrays.asList((Object[]) array));
        new Search().in("foo", new String[]{});
        log.info("AAA");
    }

    @Test
    public void testWrap() {
        val quoMark = "`";
        val regexp = "(?<!`)[^.`]+(?!`)";

        Assert.assertTrue("a".replaceAll(regexp, quoMark + "$0" + quoMark).equals("`a`"));
        Assert.assertTrue("ab".replaceAll(regexp, quoMark + "$0" + quoMark).equals("`ab`"));
        Assert.assertTrue("ab.ac".replaceAll(regexp, quoMark + "$0" + quoMark).equals("`ab`.`ac`"));
        Assert.assertTrue("ab-ac.ac-ad".replaceAll(regexp, quoMark + "$0" + quoMark).equals("`ab-ac`.`ac-ad`"));
        Assert.assertTrue("`ab-ac`.ac-ad".replaceAll(regexp, quoMark + "$0" + quoMark).equals("`ab-ac`.`ac-ad`"));
    }
}
