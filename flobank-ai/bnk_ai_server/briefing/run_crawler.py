from hk_crawler import crawl_hk
from mk_crawler import crawl_mk

MK_TRADE = "https://www.mk.co.kr/news/economy/trade/"
MK_FX = "https://www.mk.co.kr/news/economy/foreign-exchange/"

# ---------------------------------
# HK (Hankyung)
# ---------------------------------
crawl_hk("macro", "oneday")
crawl_hk("macro", "recent5")

crawl_hk("forex", "oneday")
crawl_hk("forex", "recent5")

# ---------------------------------
# MK (Maekyung)
# ---------------------------------
crawl_mk("trade", MK_TRADE, "oneday")
crawl_mk("trade", MK_TRADE, "recent5")

crawl_mk("fx", MK_FX, "oneday")
crawl_mk("fx", MK_FX, "recent5")
