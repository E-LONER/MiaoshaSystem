# 秒杀系统项目整理

[TOC]


---

## 项目框架

![image-20210302203112675](C:\Users\E-loner\AppData\Roaming\Typora\typora-user-images\image-20210302203112675.png)

**可以改进成如此（网络）**

<img src="https://tva1.sinaimg.cn/large/006y8mN6ly1g92dbyzsm7j30u80tqq4t.jpg" alt="img" style="zoom:50%;" />

秒杀系统抽象来说就是以下几个步骤：

- 用户选定商品下单

- 校验库存

- 扣库存

- 创建用户订单

- 用户支付等后续步骤…

- ---

## **整体思考**

>  秒杀无外乎解决两个核心问题，一是并发读，一是并发写

首先从高维度出发，整体思考问题。秒杀无外乎解决两个核心问题，一是并发读，一是并发写，对应到架构设计，就是高可用、一致性和高性能的要求。关于秒杀系统的设计思考，本文即基于此 3 层依次推进，简述如下——

- 高性能。 秒杀涉及高读和高写的支持，如何支撑高并发，如何抵抗高IOPS？核心优化理念其实是类似的：**高读就尽量"少读"或"读少"**，**高写就数据拆分。本文将从动静分离、热点优化以及服务端性能优化 3 个方面展开**
- 一致性。 秒杀的核心关注是商品库存，有限的商品在同一时间被多个请求同时扣减，而且要保证准确性，显而易见是一个难题。如何做到既不多又不少？本文将从业界通用的几种减库存方案切入，讨论一致性设计的核心逻辑
- 高可用。 大型分布式系统在实际运行过程中面对的工况是非常复杂的，业务流量的突增、依赖服务的不稳定、应用自身的瓶颈、物理资源的损坏等方方面面都会对系统的运行带来大大小小的的冲击。如何保障应用在复杂工况环境下还能高效稳定运行，如何预防和面对突发问题，系统设计时应该从哪些方面着手？本文将从架构落地的全景视角进行关注思考

#### **高性能**

##### **1 动静分离**

大家可能会注意到，秒杀过程中你是不需要刷新整个页面的，只有时间在不停跳动。这是因为一般都会对大流量的秒杀系统做系统的静态化改造，即数据意义上的动静分离。**动静分离三步走：1、数据拆分；2、静态缓存；3、数据整合。**

###### **1.1 数据拆分**

动静分离的首要目的是将动态页面改造成适合缓存的静态页面。因此第一步就是分离出动态数据，主要从以下 2 个方面进行：

1. 用户。用户身份信息包括登录状态以及登录画像等，相关要素可以单独拆分出来，通过动态请求进行获取；与之相关的广平推荐，如用户偏好、地域偏好等，同样可以通过异步方式进行加载
2. 时间。秒杀时间是由服务端统一管控的，可以通过动态请求进行获取 这里你可以打开电商平台的一个秒杀页面，看看这个页面里都有哪些动静数据。

###### **1.2 静态缓存**

分离出动静态数据之后，第二步就是将静态数据进行合理的缓存，由此衍生出两个问题：1、怎么缓存；2、哪里缓存

**1.2.1 怎么缓存**

静态化改造的一个特点是直接缓存整个 HTTP 连接而不是仅仅缓存静态数据，如此一来，Web 代理服务器根据请求 URL，可以直接取出对应的响应体然后直接返回，响应过程无需重组 HTTP 协议，也无需解析 HTTP 请求头。而作为缓存键，URL唯一化是必不可少的，只是对于商品系统，URL 天然是可以基于商品 ID 来进行唯一标识的，比如淘宝的 [https://item.taobao.com/item...](https://link.zhihu.com/?target=https%3A//item.taobao.com/item...).。

**1.2.2 哪里缓存**

静态数据缓存到哪里呢？可以有三种方式：1、浏览器；2、CDN ；3、服务端。

**浏览器当然是第一选择**，但用户的浏览器是不可控的，主要体现在如果用户不主动刷新，系统很难主动地把消息推送给用户（注意，当讨论静态数据时，潜台词是 “相对不变”，言外之意是 “可能会变”），如此可能会导致用户端在很长一段时间内看到的信息都是错误的。对于秒杀系统，保证缓存可以在秒级时间内失效是不可或缺的。

服务端主要进行动态逻辑计算及加载，本身并不擅长处理大量连接，每个连接消耗内存较多，同时 Servlet 容器解析 HTTP 较慢，容易侵占逻辑计算资源；另外，静态数据下沉至此也会拉长请求路径。

因此通常将静态数据缓存在 CDN，其本身更擅长处理大并发的静态文件请求，既可以做到主动失效，又离用户尽可能近，同时规避 Java 语言层面的弱点。需要注意的是，上 CDN 有以下几个问题需要解决：

1. 失效问题。任何一个缓存都应该是有时效的，尤其对于一个秒杀场景。所以，系统需要保证全国各地的 CDN 在秒级时间内失效掉缓存信息，这实际对 CDN 的失效系统要求是很高的
2. 命中率问题。高命中是缓存系统最为核心的性能要求，不然缓存就失去了意义。如果将数据放到全国各地的 CDN ，势必会导致请求命中同一个缓存的可能性降低，那么命中率就成为一个问题

因此，将数据放到全国所有的 CDN 节点是不太现实的，失效问题、命中率问题都会面临比较大的挑战。更为可行的做法是选择若干 CDN 节点进行静态化改造，节点的选取通常需要满足以下几个条件：

1. 临近访问量集中的地区
2. 距离主站较远的地区
3. 节点与主站间网络质量良好的地区

基于以上因素，选择 CDN 的二级缓存比较合适，因为二级缓存数量偏少，容量也更大，访问量相对集中，这样就可以较好解决缓存的失效问题以及命中率问题，是当前比较理想的一种 CDN 化方案。部署方式如下图所示：



<img src="https://pic4.zhimg.com/80/v2-74d40c524f50aa1574ebb8fed130af7f_1440w.jpg" alt="img" style="zoom: 33%;" />



###### **1.3 数据整合**

分离出动静态数据之后，前端如何组织数据页就是一个新的问题，主要在于动态数据的加载处理，通常有两种方案：ESI（Edge Side Includes）方案和 CSI（Client Side Include）方案。

1. ESI 方案：Web 代理服务器上请求动态数据，并将动态数据插入到静态页面中，用户看到页面时已经是一个完整的页面。这种方式对服务端性能要求高，但用户体验较好
2. CSI 方案：Web 代理服务器上只返回静态页面，前端单独发起一个异步 JS 请求动态数据。这种方式对服务端性能友好，但用户体验稍差



##### **2 热点优化**

热点分为热点操作和热点数据，以下分开进行讨论。

###### **热点隔离**

热点数据识别出来之后，第一原则就是将热点数据隔离出来，不要让 1% 影响到另外的 99%，可以基于以下几个层次实现热点隔离：

1. 业务隔离。秒杀作为一种营销活动，卖家需要单独报名，从技术上来说，系统可以提前对已知热点做缓存预热
2. 系统隔离。系统隔离是运行时隔离，通过分组部署和另外 99% 进行分离，另外秒杀也可以申请单独的域名，入口层就让请求落到不同的集群中
3. 数据隔离。秒杀数据作为热点数据，可以启用单独的缓存集群或者DB服务组，从而更好的实现横向或纵向能力扩展

###### **热点优化**

热点数据隔离之后，也就方便对这 1% 的请求做针对性的优化，方式无外乎两种：

1. 缓存：热点缓存是最为有效的办法。如果热点数据做了动静分离，那么可以长期缓存静态数据
2. 限流：流量限制更多是一种保护机制。需要注意的是，各服务要时刻关注请求是否触发限流并及时进行review



---

#### **一致性**

秒杀系统中，库存是个关键数据，卖不出去是个问题，超卖更是个问题。秒杀场景下的一致性问题，主要就是库存扣减的准确性问题。

##### **1 减库存的方式**

电商场景下的购买过程一般分为两步：下单和付款。“提交订单”即为下单，“支付订单”即为付款。基于此设定，减库存一般有以下几个方式：

1. 下单减库存。买家下单后，扣减商品库存。下单减库存是最简单的减库存方式，也是控制最为精确的一种
2. 付款减库存。买家下单后，并不立即扣减库存，而是等到付款后才真正扣减库存。但因为付款时才减库存，如果并发比较高，可能出现买家下单后付不了款的情况，因为商品已经被其他人买走了
3. 预扣库存。这种方式相对复杂一些，买家下单后，库存为其保留一定的时间（如 15 分钟），超过这段时间，库存自动释放，释放后其他买家可以购买
4. 

##### **2 减库存的问题**

###### **2.1 下单减库存**

优势：用户体验最好。下单减库存是最简单的减库存方式，也是控制最精确的一种。下单时可以直接通过数据库事务机制控制商品库存，所以一定不会出现已下单却付不了款的情况。

劣势：可能卖不出去。正常情况下，买家下单后付款概率很高，所以不会有太大问题。但有一种场景例外，就是当卖家参加某个促销活动时，竞争对手通过恶意下单的方式将该商品全部下单，导致库存清零，那么这就不能正常售卖了——要知道，恶意下单的人是不会真正付款的，这正是 “下单减库存” 的不足之处。

###### **2.2 付款减库存**

优势：一定实际售卖。“下单减库存” 可能导致恶意下单，从而影响卖家的商品销售， “付款减库存” 由于需要付出真金白银，可以有效避免。

劣势：用户体验较差。用户下单后，不一定会实际付款，假设有 100 件商品，就可能出现 200 人下单成功的情况，因为下单时不会减库存，所以也就可能出现下单成功数远远超过真正库存数的情况，这尤其会发生在大促的热门商品上。如此一来就会导致很多买家下单成功后却付不了款，购物体验自然是比较差的。

###### **2.3 预扣库存**

优势：缓解了以上两种方式的问题。预扣库存实际就是“下单减库存”和 “付款减库存”两种方式的结合，将两次操作进行了前后关联，下单时预扣库存，付款时释放库存。

劣势：并没有彻底解决以上问题。比如针对恶意下单的场景，虽然可以把有效付款时间设置为 10 分钟，但恶意买家完全可以在 10 分钟之后再次下单。



##### **3 实际如何减库存**

业界最为常见的是预扣库存。无论是外卖点餐还是电商购物，下单后一般都有个 “有效付款时间”，超过该时间订单自动释放，这就是典型的预扣库存方案。但如上所述，预扣库存还需要解决恶意下单的问题，保证商品卖的出去；另一方面，如何避免超卖，也是一个痛点。

1. 卖的出去：恶意下单的解决方案主要还是结合安全和反作弊措施来制止。比如，识别频繁下单不付款的买家并进行打标，这样可以在打标买家下单时不减库存；再比如为大促商品设置单人最大购买件数，一人最多只能买 N 件商品；又或者对重复下单不付款的行为进行次数限制阻断等
2. 避免超卖：库存超卖的情况实际分为两种。对于普通商品，秒杀只是一种大促手段，即使库存超卖，商家也可以通过补货来解决；而对于一些商品，秒杀作为一种营销手段，完全不允许库存为负，也就是在数据一致性上，需要保证大并发请求时数据库中的库存字段值不能为负，一般有多种方案：一是在通过事务来判断，即保证减后库存不能为负，否则就回滚；二是直接设置数据库字段类型为无符号整数，这样一旦库存为负就会在执行 SQL 时报错；三是使用 CASE WHEN 判断语句：`sql UPDATE item SET inventory = CASE WHEN inventory >= xxx THEN inventory-xxx ELSE inventory END`

业务手段保证商品卖的出去，技术手段保证商品不会超卖，库存问题从来就不是简单的技术难题，解决问题的视角是多种多样的。



---

#### 高可用



盯过秒杀流量监控的话，会发现它不是一条蜿蜒而起的曲线，而是一条挺拔的直线，这是因为秒杀请求高度集中于某一特定的时间点。这样一来就会造成一个特别高的零点峰值，而对资源的消耗也几乎是瞬时的。所以秒杀系统的可用性保护是不可或缺的。

##### **1 流量削峰**

对于秒杀的目标场景，最终能够抢到商品的人数是固定的，无论 100 人和 10000 人参加结果都是一样的，即有效请求额度是有限的。并发度越高，无效请求也就越多。但秒杀作为一种商业营销手段，活动开始之前是希望有更多的人来刷页面，只是真正开始后，秒杀请求不是越多越好。因此系统可以设计一些规则，人为的延缓秒杀请求，甚至可以过滤掉一些无效请求。

###### **1.1 前台校验码**

早期秒杀只是简单的点击秒杀按钮，后来才增加了答题。为什么要增加答题呢？主要是通过提升购买的复杂度，达到两个目的：

1. 防止作弊。早期秒杀器比较猖獗，存在恶意买家或竞争对手使用秒杀器扫货的情况，商家没有达到营销的目的，所以增加答题来进行限制
2. 延缓请求。零点流量的起效时间是毫秒级的，答题可以人为拉长峰值下单的时长，由之前的 <1s 延长到 <10s。这个时间对于服务端非常重要，会大大减轻高峰期并发压力；另外，由于请求具有先后顺序，答题后置的请求到来时可能已经没有库存了，因此根本无法下单，此阶段落到数据层真正的写也就非常有限了

需要注意的是，答题除了做正确性验证，还需要对提交时间做验证，比如<1s 人为操作的可能性就很小，可以进一步防止机器答题的情况。

答题目前已经使用的非常普遍了，本质是通过在入口层削减流量，从而让系统更好地支撑瞬时峰值。



###### **1.2 排队**

最为常见的削峰方案是使用消息队列，通过把同步的直接调用转换成异步的间接推送缓冲瞬时流量。除了消息队列，类似的排队方案还有很多，例如：

1. 线程池加锁等待
2. 本地内存蓄洪等待
3. 本地文件序列化写，再顺序读

排队方式的弊端也是显而易见的，主要有两点：

1. 请求积压。流量高峰如果长时间持续，达到了队列的水位上限，队列同样会被压垮，这样虽然保护了下游系统，但是和请求直接丢弃也没多大区别
2. 用户体验。异步推送的实时性和有序性自然是比不上同步调用的，由此可能出现请求先发后至的情况，影响部分敏感用户的购物体验

排队本质是在业务层将一步操作转变成两步操作，从而起到缓冲的作用，但鉴于此种方式的弊端，最终还是要基于业务量级和秒杀场景做出妥协和平衡。



###### **1.3 过滤**

过滤的核心结构在于分层，通过在不同层次过滤掉无效请求，达到数据读写的精准触发。常见的过滤主要有以下几层：

1. 读限流：对读请求做限流保护，将超出系统承载能力的请求过滤掉
2. 读缓存：对读请求做数据缓存，将重复的请求过滤掉
3. 写限流：对写请求做限流保护，将超出系统承载能力的请求过滤掉
4. 写校验：对写请求做一致性校验，只保留最终的有效数据

过滤的核心目的是通过减少无效请求的数据IO保障有效请求的IO性能。



##### **2 Plan B**

当一个系统面临持续的高峰流量时，其实是很难单靠自身调整来恢复状态的，日常运维没有人能够预估所有情况，意外总是无法避免。尤其在秒杀这一场景下，为了保证系统的高可用，必须设计一个 Plan B 方案来进行兜底。

高可用建设，其实是一个系统工程，贯穿在系统建设的整个生命周期。



![img](https://pic2.zhimg.com/80/v2-0006fa1a5791f8341cc998defaac7a29_1440w.jpg)



具体来说，系统的高可用建设涉及架构阶段、编码阶段、测试阶段、发布阶段、运行阶段，以及故障发生时，逐一进行分析：

1. 架构阶段：考虑系统的可扩展性和容错性，避免出现单点问题。例如多地单元化部署，即使某个IDC甚至地市出现故障，仍不会影响系统运转
2. 编码阶段：保证代码的健壮性，例如RPC调用时，设置合理的超时退出机制，防止被其他系统拖垮，同时也要对无法预料的返回错误进行默认的处理
3. 测试阶段：保证CI的覆盖度以及Sonar的容错率，对基础质量进行二次校验，并定期产出整体质量的趋势报告
4. 发布阶段：系统部署最容易暴露错误，因此要有前置的checklist模版、中置的上下游周知机制以及后置的回滚机制
5. 运行阶段：系统多数时间处于运行态，最重要的是运行时的实时监控，及时发现问题、准确报警并能提供详细数据，以便排查问题
6. 故障发生：首要目标是及时止损，防止影响面扩大，然后定位原因、解决问题，最后恢复服务

对于日常运维而言，高可用更多是针对运行阶段而言的，此阶段需要额外进行加强建设，主要有以下几种手段：

1. 预防：建立常态压测体系，定期对服务进行单点压测以及全链路压测，摸排水位
2. 管控：做好线上运行的降级、限流和熔断保护。需要注意的是，无论是限流、降级还是熔断，对业务都是有损的，所以在进行操作前，一定要和上下游业务确认好再进行。就拿限流来说，哪些业务可以限、什么情况下限、限流时间多长、什么情况下进行恢复，都要和业务方反复确认
3. 监控：建立性能基线，记录性能的变化趋势；建立报警体系，发现问题及时预警
4. 恢复：遇到故障能够及时止损，并提供快速的数据订正工具，不一定要好，但一定要有 在系统建设的整个生命周期中，每个环节中都可能犯错，甚至有些环节犯的错，后面是无法弥补的或者成本极高的。所以高可用是一个系统工程，必须放到整个生命周期中进行全面考虑。同时，考虑到服务的增长性，高可用更需要长期规划并进行体系化建设。





---

## 需要解决的问题

### 1、超卖

- 超卖——生成的订单数量超出库存数量

  原因：假设某个抢购场景中，我们一共只有100个商品，在最后一刻，我们已经消耗了99个商品，仅剩最后一个。这个时候，系统发来多个并发请求，这批请求读取到的商品余量都是1个，然后都通过了这一个余量判断，最终导致超发。

  **原因总结**：线程不安全



- 解决方案

  - **悲观锁思维**：但凡有生成订单请求进来读取库存生成订单都对数据加锁，采用锁定状态，排斥外部请求的修改。遇到加锁的状态，就必须等待。
    
  ```sql
     select * from emp where empid > 100 for update;
  ```
  
    - 局限性：秒杀系统是高并发系统，读取修改加锁的数据的请求数量大，瞬间增大系统的平均响应时间，结果是可用连接数被耗尽，系统陷入异常。
    
  -  **FIFO队列思路**：创建一个系统启动时就创建一个FIFO队列，直接将请求放入队列中的，采用FIFO（First Input First Output，先进先出），这样的话，就不会导致某些请求永远获取不到锁
    
    - 局限性：高并发的场景下，因为请求很多，很可能一瞬间将队列内存“撑爆”，然后系统又陷入到了异常状态
    
  - **乐观锁思维**：给库存创建一个版本号，每次修改前判断版本号时候改变



---

### 恶意请求

**什么是恶意请求：**那简单啊，我知道你什么时候抢，我搞个几十台机器搞点脚本，我也模拟出来十几万个人左右的请求，那我是不是意味着我基本上有80%的成功率了。真实情况可能远远不止，因为机器请求的速度比人的手速往往快太多了









---

### 链接暴露

**什么是链接暴露：**懂点行的仔都可以打开谷歌的开发者模式，然后看看你的网页代码，有的就有URL，但是我写VUE的时候是事件触发然后去调用文件里面的接口看源码看不到，但是我可以点击一下查看你的请求地址啊，不过你好像可以对按钮在秒杀前置灰。

不管怎么样子都有危险，撇开外面的所有的东西你都挡住了，你卖这个东西实在便宜得过分，有诱惑力，你能保证开发不动心？开发知道地址，在秒杀的时候自己提前请求。







---

### 数据库崩溃

**什么是数据库崩溃：**每秒上万甚至十几万的**QPS**（每秒请求数）直接打到**数据库**，基本上都要把库打挂掉，而且你服务不单单是做秒杀的还涉及其他的业务，你没做**降级、限流、熔断**啥的，别的一起挂，小公司的话可能**全站崩溃404**





---

---

## 1、秒杀接口设计

#### 预缓存热点数据

将库存在服务器启动时添加到redis服务器中，并设置TTL;

```java
 @Override
public void afterPropertiesSet() throws Exception {
      List<GoodsVo> goodsList=goodsService.getGoodsInfo();
      for(GoodsVo goods:goodsList){
				        redisService.set(Goods.getGoodsStock(String.valueOf(goods.getId())).getPrefix(),60*3,goods.getStockCount());
      stockMark.put(goods.getId(),true);
            }
}
```



#### 后端限流

秒杀的时候肯定是涉及到后续的订单生成和支付等操作，但是都只是成功的幸运儿才会走到那一步，那一旦100个产品卖光了，return了一个false，前端直接秒杀结束，然后你后端也关闭后续无效请求的介入了。



#### 使用标记内存法减少redis访问

使用标记法减少请求过多访问Redis服务器，即在接口内创建一个内部实例变量，并让秒杀接口实现InitializingBean得afterPropertiesSet()方法，针对每个秒杀货品，用货品ID作为key，初始化值为true（即库存充足）

在接收到请求后，先判断标记得库存是否充足，否则直接返回失败信息——库存不足

否则，下一步继续查询redis中缓存得库存，如果充足，则标记货品对应得key得值为true，否则标记为false

```java
 // 内存标记法优化，即减少redis访问
 private Map<Long,Boolean> stockMark=new HashMap<>();
 
  @Override
public void afterPropertiesSet() throws Exception {
      List<GoodsVo> goodsList=goodsService.getGoodsInfo();
      for(GoodsVo goods:goodsList){
				        redisService.set(Goods.getGoodsStock(String.valueOf(goods.getId())).getPrefix(),60*3,goods.getStockCount());
      stockMark.put(goods.getId(),true);
            }
}
```



#### 接口拦截多次访问

使用自定义拦截器注解判断接口处判断请求是否短时间内多次访问

```java
@AccessLimit(time = 10,maxAccess = 5,login = true)
```

写注解

```java
//注解@Retention可以用来修饰注解，是注解的注解，称为元注解。
//有三个参数：CLASS  RUNTIME   SOURCE
//SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；如果只是做一些检查性的操作，比如 @Override 和 @SuppressWarnings，使用SOURCE 注解。
//CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；如果要在编译时进行一些预处理操作，比如生成一些辅助代码（如 ButterKnife），就用 CLASS注解；
//RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；运行时去动态获取注解信息，那只能用 RUNTIME 注解
//生命周期长度 SOURCE < CLASS < RUNTIME ，所以前者能作用的地方后者一定也能作用。
@Retention(RUNTIME)
//@Target说明了Annotation所修饰的对象范围：Annotation可被用于 packages、types（类、接口、枚举、Annotation类型）、类型成员（方法、构造方法、成员变量、枚举值）、方法参数和本地变量（如循环变量、catch参数）。在Annotation类型的声明中使用了target可更加明晰其修饰的目标。
@Target(METHOD)
public @interface AccessLimit {
    int time();
    int maxAccess();
    boolean login() default true;
}
```

注解接口实现

```java
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserService userService;

    @Override
    //方法调用前的拦截器，即实现请求频繁注解，写完注解主体后，记得要在config包下的WebConfig类中注册注解，才可以调用
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (handler instanceof HandlerMethod){
            MiaoshaUser user=getUser(request,response);
            UserContext_localThread.setUser(user);
            HandlerMethod handlerMethod= (HandlerMethod) handler;//创建一个handler对象
            AccessLimit accessLimit=handlerMethod.getMethodAnnotation(AccessLimit.class);//获取自己创建的注解对象
            if (accessLimit ==null)
                return true;
            boolean login = accessLimit.login();
            int maxAccess = accessLimit.maxAccess();
            int time = accessLimit.time();
            String uri_key=request.getRequestURI();
            if (login && user ==null){
                sendMsg(Result.error(CodeMsg.loginUserNotExit),response);
                return false;
            }else if (login && user !=null){
                uri_key+=":"+user.getId();
            }else{

            }
            String key=AccessKey.getById(uri_key).getPrefix();
            long access_count=redisService.get(key,Long.class);
            if (access_count==0){
                redisService.set(key,time,1);
            }else if(access_count<maxAccess){
                redisService.decr(key);
            }else {
                sendMsg(Result.error(CodeMsg.Access_ERR),response);
                return false;
            }


        }
        return true;
    }

```



#### 使用RabbitMQ异步下单

1. 请求进来后先由内存标记判断秒杀的商品是否还有库存，无则直接返回库存不足消息

2. 内存标记显示库存足够，则再次判断Redis库存是否还有，无则返回库存不足消息

3. 使用RabbitMQ消息队列异步下单，返回正在排队中消息

   ```java
   //将请求加入消息队列中
               MiaoshaMsg msg = new MiaoshaMsg();
               msg.setGoodsId(goodsId);
               msg.setUserId(uId);
               mQsender.sendMiaoshaMsg(msg);
               return Result.success(0);
   ```

4. MQ接受到消息，同步处理队列元素

   ```java
   public void receive(String msgs){
           log.info("MQ收到信息："+msgs);
           MiaoshaMsg msg= ObjectConverter.StringTo(msgs,MiaoshaMsg.class);
           long userId=msg.getUserId();
           long goodsId=msg.getGoodsId();
   
           //查询库存
           GoodsVo goods=goodsService.getGoodsDetail(goodsId);
           if(goods.getStockCount()<=0){
   //                log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀失败，库存不足！");
                   return ;
               }
           //判断是否秒杀成功
           MiaoshaOrder order=orderService.getOrderByUidGid(userId,goodsId);
           if(order !=null){
               return;
           }
   
           Order orderInfo=miaoshaService.miaosha(userId,goods);
               if (orderInfo != null){
                   return;
               }
   
       }
   }
   ```








---

## 2、资源静态化

#### 1、cdn服务器：

秒杀一般都是特定的商品还有页面模板，现在一般都是前后端分离的，所以页面一般都是不会经过后端的，但是前端也要自己的服务器啊，那就把能提前放入**cdn服务器**的东西都放进去，反正把所有能提升效率的步骤都做一下，减少真正秒杀时候服务器的压力。



#### 2、静态页面Redis缓存

1.页面缓存思路：

> 首先我们需要明白，一个页面是从后端提高数据后，交给springMvc或者SpringBoot进行渲染，主要的页面消耗是在渲染这部分。因此我们需要在这之前进行拦截。(针对于依靠后端进行页面跳转和渲染的缓存初始阶段)
>
> 核心通用逻辑：当客户的请求到达后端时，先去redis中查询缓存，如果缓存中找不到，则进行数据库逻辑操作，然后渲染，存入缓存并返回给前端!如果在缓存中找到了则直接返回给前端。存储在Redis缓存中的页面需要设置超时时间，缓存的时间长度根据页面数据变化频繁程度适当调整!目前大多数页面缓存都是在60~120秒，少数几乎不变化的可以调整到5分钟!

**Redis页面缓存实现**

1.请求到来后，先调用根据key值去redis访问，找到则返回

2.SpringBoot的redis中找不到调用webConext()对象进行加载

1. 将Thyemleaf的thymeleafViewResolver给注入到Controller中

2. 进行页面渲染:

   利用ThymeleafViewResolver接口实现渲染

   ```java
   WebContext context=new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
   goodslist_html=thymeleafViewResolver.getTemplateEngine().process("goods_list", context);
   if(!StringUtils.isEmpty(goodslist_html))
       redisService.set(Goods.getById("").getPrefix(),Goods.getExpire(),goodslist_html);
   return goodslist_html;
   ```

   

   WebContext ctx=new

   WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());

   ```
    html=thymeleafViewResolver.getTemplateEngine().process("页面名称无后缀",ctx);
   ```

2.页面局部缓存:

```
热点数据缓存，页面静态化进行ajax请求信息更新，此类信息一般都是比较频繁发生变化的，涉及的可能是需要保存在数据库的操作,类似表格信息，即时刷新的数据等！如果是属于查看类的并且前端大量请求，可以经由于后端监控，定时写入缓存!

核心通用逻辑：一般情况下封装以类名--对象名为组合的字符串作为Redis的Key值，然后存入数据库，每次访问到目标的方法都先去缓存读取，然后再处理！
```



#### 3、资源缓存到游览器，ajax请求变量值

> 将页面静态化的特点必须解决页面如何获取与处理数据，如何跳转页面的问题！在此我们可以参考 ajax技术 ，将请求与页面完全独立，保证页面是静态页面，而请求通过Ajax技术局部刷新与全局刷新的特点来实现!变化如下:
>
> 1----原流程？
> <img src="https://img2018.cnblogs.com/blog/1348997/202002/1348997-20200211201229083-443937567.png" alt="img" style="zoom: 80%;" />
>
> 2----静态化页面流程?
> <img src="https://img2018.cnblogs.com/blog/1348997/202002/1348997-20200211201255859-1128567425.png" alt="img" style="zoom:80%;" />
>
> 注意:此流程为页面静态化的流程，前端的页面跳转将由页面之间直接完成，不在通过后端!页面之间的数据加载，则是通过Ajax请求的形式，请求后端返回JSON数据流。在请求中有缓存则显访问缓存，没有缓存或者缓存超时则直接访问下一层，知道完整返回数据!
>
> 3------浏览器设置与分析？
> 一般情况下，一个正常的请求都具备了请求报文和响应报文，请求和响应均分成三个部分，请求头则维护了请求协议类型，而请求和响应报文则维护了对于报文体的参数，生命，来源等信息!第三部分则是本次请求的内容，也就是我们说的报文体!对于浏览器来说，他更像是一个负责页面渲染和参数设定的容器，读取报文头的信息对报问题进行相关的操作，并通过一定的通过规则展示给使用者!
>
> 浏览器的主要功能是将用户选择的web资源呈现出来，它需要从服务器请求资源，并将其显示在浏览器窗口中，资源的格式通常是HTML，也包括PDF、image及其他格式。　HTML和CSS规范中规定了浏览器解释html文档的方式，由W3C组织对这些规范进行维护，W3C是负责制定web标准的组织。
>
> 关于浏览器原理可以参考：https://kb.cnblogs.com/page/129756/
>
> 页面静态化的一个特点也是可以将静态页面给缓存在用户浏览器一端。同一个页面，在页面的生命周期没到之前，用户的请求都是在本地进行(304)；
>
> 用于识别是否缓存并且区分缓存时间的主要是以下三个模块：
>
> Pragma：支持http1.0版本的缓存
> Expire：http2.0可以用，也向下兼容http1.0版本，16进制的字符串，以格里尼治时间也准，以服务端时间来定义缓存你是否超时，但由于客户端与服务端缓存时间经常不一致，所以容易造成缓存失效!
> Cache-control：Http版本1.0-2.0都可以用，以秒为单位，并且可以指定缓存为多少秒，对缓存时间进行倒计时，同时不会根据客户端时间来衡量，也不会根据服务端时间衡量，完全依赖信息本身!
>
> cache-control=max-age=3600：服务端告诉浏览器指定3600秒
> 观察：在网络XHR上面的连接输出字段可以看到连接已经缓存，在缓存有效期内，每一次请求都是返回304，但只是浏览器自己处理，实际请求并没有到达后端系统!



#### 4、静态资源优化?

除了对静态页面优化，我们还可以通过一些方式减少流量，提供访问的速度!当用户的请求越小，性能也就相对越好!
1.JS/CSS压缩，静态资源尽量使用压缩版的库和包，以减少浏览器加载和请求的流量
\2. 多个js/css组合到一个请求，减少连接数（正常30个，从服务端获取，多次访问，通过http获取），把多个文件通过一个js/css一次性请求下来 配置 tengine模块实现!

3.将多个Js/css的请求合并为一个
4.CDN：内容分发网络，将数据缓存到网络节点上，用户请求来根据位置定向访问到距离最近的节点，可用于解决网络拥挤，跟代码层面关系不是很大，在请求没到网站之前，CDN会根据客户的位置将请求分发到就近地网路节点上，如果节点有则直接返回!

---

## 3、热点数据缓存

2.热点数据缓存思路:

>   所谓热点数据，就是指在某段时间内被频繁使用的对象数据。比如用户登录信息,用户在登录后，每次访问都会携带其cookie信息进入后端，当信息到达后端后，其cookie信息就是我们存在redis中的key值。在这一步我们会做四个操作，并且在某些时候可使用拦截器进行处理：
>
>    <1>当用户操作进来的时候，我们获取到Cookie值并在Redis中查找，找到用户信息则刷新用户的登录时间并允许用户通过，找不到用户信息则拒绝用户继续往下!，在后面的数据操作中，如果存在需要使用用户信息的操作，则去Redis中查找，如果存在则允许操作!
>
>    <2>对于热点数据源，被高频访问的不缺分权限信息的热点数据，则设置全局缓存，定时更新则缓存数据，当有操作到此类的热点数据缓存则主动更新缓存中的信息，将用户拦截在数据之外!
>
>    <3>当涉及到用户登录的热点数据被更新后，需要根据用户的token作为key值重新写入或者强制用户重新登录！
>
>    <4>对于需要频繁更新的数据或写入数据的数据，比如点赞次数，在线人数，可以设置一个层级，在没有达到层级前写在缓存中，每次只更新缓存则可以，当到一定次数则写入数据库!

2.页面局部缓存



---

## 4、前台措施

#### 按钮控制：

大家有没有发现没到秒杀前，一般按钮都是置灰的，只有时间到了，才能点击。

这是因为怕大家在时间快到的最后几秒秒疯狂请求服务器，然后还没到秒杀的时候基本上服务器就挂了。

这个时候就需要前端的配合，定时去请求你的后端服务器，获取最新的北京时间，到时间点再给按钮可用状态。



#### **前端限流**：

1、这个很简单，一般秒杀不会让你一直点的，一般都是点击一下或者两下然后几秒之后才可以继续点击，这也是保护服务器的一种手段。

2、图片验证码，使用网上模板



---

## 可以改进得地方

#### Redis集群：

之前不是说单机的**Redis**顶不住嘛，那简单多找几个兄弟啊，秒杀本来就是读多写少，那你们是不是瞬间想起来我之前跟你们提到过的，**Redis集群**，**主从同步**、**读写分离**，我们还搞点**哨兵**，开启**持久化**直接无敌高可用！

<img src="https://tva1.sinaimg.cn/large/006y8mN6ly1g8p9gedwerj312y0hodhm.jpg" alt="img" style="zoom:50%;" />



**Redis得一个问题：**

采用**主从**，就是我们会去读取库存然后再判断然后有库存才去减库存，正常情况没问题，但是高并发的情况问题就很大了。就比如现在库存只剩下1个了，我们高并发嘛，4个服务器一起查询了发现都是还有1个，那大家都觉得是自己抢到了，就都去扣库存，那结果就变成了-3，是的只有一个是真的抢到了，别的都是超卖的。咋办？

**Lua：**

> **Lua** 脚本功能是 Reids在 2.6 版本的最大亮点， 通过内嵌对 Lua 环境的支持， Redis 解决了长久以来不能高效地处理 **CAS** （check-and-set）命令的缺点， 并且可以通过组合使用多个命令， 轻松实现以前很难实现或者不能高效实现的模式。
>
> **Lua脚本是类似Redis事务，有一定的原子性，不会被其他命令插队，可以完成一些Redis事务性的操作。**

写一个脚本把判断库存扣减库存的操作都写在一个脚本丢给Redis去做，那到0了后面的都Return False了是吧，一个失败了你修改一个开关，直接挡住所有的请求



#### 限流&降级&熔断&隔离：

这个为啥要做呢，不怕一万就怕万一，万一你真的顶不住了，**限流**，顶不住就挡一部分出去但是不能说不行，**降级**，降级了还是被打挂了，**熔断**，至少不要影响别的系统，**隔离**，你本身就独立的，但是你会调用其他的系统嘛，你快不行了你别拖累兄弟们啊。

![img](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1573745703045&di=af0071ba36eace57ad770eca350b5b68&imgtype=0&src=http%3A%2F%2Fimg.bqatj.com%2Fimg%2F8bb02435ae40b2f2.jpg)

#### Nginx：

Nginx大家想必都不陌生了吧，这玩意是高性能的web服务器，并发也随便顶几万不是梦，但是我们的Tomcat只能顶几百的并发呀，那简单呀负载均衡嘛，一台服务几百，那就多搞点，在秒杀的时候多租点流量机。

<img src="https://tva1.sinaimg.cn/large/006y8mN6ly1g8yylq6f3mj30vs0hyq41.jpg" alt="img" style="zoom:50%;" />



#### 缓存雪崩

> 缓存雪崩：缓存雪崩是指因为数据未加载到缓存中，或者缓存同一时间大面积的失效，在某一时刻大量的缓存没有命中，从而导致所有请求都去查数据库，导致数据库CPU和内存负载过高，甚至宕机!



**解决办法**

1.设置随机数Rdis TTL过期时间：可以防止大量数据同一时间过期带来的缓存雪崩问题。

2.缓存失效：如果缓存集中在一段时间内失效，DB的压力凸显，DB负载急剧上升。这个没有完美解决办法，但可以分析用户行为，尽量让失效时间点均匀分布。







#### 缓存穿透

> 缓存穿透：查询一个数据库必然不存在的数据，那么缓存里面也没有。比如文章表中查询一个不存在的id，每次都会访问DB，如果有人恶意破坏，发送高频请求，那么很可能直接对DB造成影响。
>
>  
>
> 缓存穿透是指查询一个一定不存在的数据，由于缓存是不命中时被动写的，并且出于容错考虑，如果从存储层查不到数据则不写入缓存，这将导致这个不存在的数据每次请求都要到存储层去查询，失去了缓存的意义。在流量大时，可能DB就挂掉了，要是有人利用不存在的key频繁攻击我们的应用，这就是漏洞。



**解决办法**：对所有可能查询的参数以hash形式存储，在控制层先进行校验，不符合则丢弃。或者对于查询为空的字段，设置一个默认值在缓存中，如果查询到则返回默认值! 或者使用具备特点的key值，如果不符合则经由于系统过滤掉，不进入缓存也不进入数据库,此做法可以降低一定的压力，但是解决不了根本的问题。



#### 缓存击穿

> 对于一些设置了过期时间的key，如果这些key可能会在某些时间点被超高并发地访问，是一种非常“热点”的数据。这个时候，需要考虑一个问题：缓存被“击穿”的问题，这个和缓存雪崩的区别在于这里针对某一key缓存，前者则是很多key。
>
> 缓存在某个时间点过期的时候，恰好在这个时间点对这个Key有大量的并发请求过来，这些请求发现缓存过期一般都会从后端DB加载数据并回设到缓存，这个时候大并发的请求可能会瞬间把后端DB压垮。

**解决方案**

- **使用互斥锁(mutex key)**
  业界比较常用的做法，是使用mutex。简单地来说，就是在缓存失效的时候（判断拿出来的值为空），不是立即去load db，而是先使用缓存工具的某些带成功操作返回值的操作（比如Redis的SETNX或者Memcache的ADD）去set一个mutex key，当操作返回成功时，再进行load db的操作并回设缓存；否则，就重试整个get缓存的方法。
  SETNX，是「SET if Not eXists」的缩写，也就是只有不存在的时候才设置，可以利用它来实现锁的效果。在redis2.6.1之前版本未实现setnx的过期时间，所以这里给出两种版本代码参考：

  ```java
  public String get(key) {
        String value = redis.get(key);
        if (value == null) { //代表缓存值过期
            //设置3min的超时，防止del操作失败的时候，下次缓存过期一直不能load db
  		  if (redis.setnx(key_mutex, 1, 3 * 60) == 1) {  //代表设置成功
                 value = db.get(key);
                        redis.set(key, value, expire_secs);
                        redis.del(key_mutex);
                } else {  //这个时候代表同时候的其他线程已经load db并回设到缓存了，这时候重试获取缓存值即可
                        sleep(50);
                        get(key);  //重试
                }
            } else {
                return value;      
            }
   }
  ```

  

- "提前"使用互斥锁(mutex key)：
  在value内部设置1个超时值(timeout1), timeout1比实际的memcache timeout(timeout2)小。当从cache读取到timeout1发现它已经过期时候，马上延长timeout1并重新设置到cache。然后再从数据库加载数据并设置到cache中。

  ```java
  v = memcache.get(key);  
  if (v == null) {  
      if (memcache.add(key_mutex, 3 * 60 * 1000) == true) {  
          value = db.get(key);  
          memcache.set(key, value);  
          memcache.delete(key_mutex);  
      } else {  
          sleep(50);  
          retry();  
      }  
  } else {  
      if (v.timeout <= now()) {  
          if (memcache.add(key_mutex, 3 * 60 * 1000) == true) {  
              // extend the timeout for other threads  
              v.timeout += 3 * 60 * 1000;  
              memcache.set(key, v, KEY_TIMEOUT * 2);  
    
              // load the latest value from db  
              v = db.get(key);  
              v.timeout = KEY_TIMEOUT;  
              memcache.set(key, value, KEY_TIMEOUT * 2);  
              memcache.delete(key_mutex);  
          } else {  
              sleep(50);  
              retry();  
          }  
      }  
  } 
  ```

  
