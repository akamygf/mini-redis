package site.hfny258.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 一个简化版的泛型字典，底层使用“数组 + 链表”实现哈希表。
 *
 * 每个哈希桶保存一条单向链表：当多个 key 经过哈希计算后落到同一个
 * 下标时，就把这些 key 串在同一个链表中，这种方式称为“拉链法”或“链地址法”。
 *
 * 这个类还实现了渐进式 rehash（重新散列）：扩容时不一次性搬迁全部数据，
 * 而是先创建新表 ht1，再由后续的读写操作逐步把旧表 ht0 中的桶迁移过去。
 * 这样可以避免一次扩容长时间阻塞单个操作。
 *
 * rehashIndex 是 rehash 状态标记：
 * <ul>
 *     <li>-1：当前没有进行 rehash，ht0 是唯一有效的哈希表；</li>
 *     <li>非 -1：当前正在 rehash，rehashIndex 表示下一个待迁移的 ht0 桶下标，
 *     ht1 是扩容后的新表。</li>
 * </ul>
 *
 * 注意：当前实现中的 put/remove 方法使用了 {@code rehashIndex != 1} 作为
 * 判断条件，而本类约定的空闲标记是 {@code -1}。这属于需要单独修正的逻辑问题，
 * 本次只添加注释，不改变原有代码行为。
 */
public class Dict<K, V> {

    /** 初始哈希表容量。容量保持为 2 的幂，便于使用位运算计算桶下标。 */
    private static final int INITAL_SIZE = 4;

    /** 负载因子阈值；当前表中元素数量超过容量的 75% 时开始扩容。 */
    private static final double LOAD_FACTOR = 0.75;

    /** 每次 rehash 最多处理的非空桶数量，避免一次操作搬迁过多数据。 */
    private static final int REHASH_MAX_SIZE = 5;

    /**
     * 每次 rehash 最多连续跳过的空桶数量。
     * 常量名中的 percent 容易造成误解：这里保存的是“数量上限”，并不是百分比。
     */
    private static final int MAX_EMPTY_PERCENT = 5;

    /**
     * 当前使用的哈希表。
     * 没有 rehash 时，所有数据都在 ht0 中；rehash 过程中，ht0 仍然保存尚未
     * 搬迁的数据，同时也可能保存已经被查询到但尚未迁移的旧桶。
     */
    private DictHashTable<K,V> ht0;

    /**
     * 扩容后创建的新哈希表。
     * 只有 rehash 进行期间才应该非空。新插入的数据直接写入 ht1，避免新数据
     * 又被写入即将废弃的旧表 ht0。
     */
    private DictHashTable<K,V> ht1;

    /** 下一个需要从 ht0 搬迁的桶下标；-1 表示当前没有进行 rehash。 */
    private int rehashIndex;

    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }
        if (rehashIndex != -1) {
            reHashStep();
        }
        return find(key) != null;
    }

    /**
     * 哈希桶中的链表节点。
     * 当多个 key 发生哈希冲突时，节点通过 next 指针连接起来。key 和 value
     * 都不要求实现特定接口，但 key 必须能够稳定地提供 hashCode 和 equals。
     */
    static class DictEntry<K, V> {
        /** 存储的键。put 方法禁止传入 null，因此正常节点的 key 不为 null。 */
        K key;

        /** 与 key 对应的值。 */
        V value;

        /** 同一个桶中下一个冲突节点；没有下一个节点时为 null。 */
        DictEntry<K, V> next;

        /** 创建一个尚未连接到桶链表中的节点。 */
        DictEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * 一个实际的哈希表。
     * table 是桶数组，数组中的每个元素是某条冲突链的头节点。size 表示
     * 桶数量，used 表示当前表内实际存放的节点数量，而不是非空桶数量。
     */
    static class DictHashTable<K, V> {
        /** 桶数组；每个位置指向该桶链表的第一个节点。 */
        private DictEntry<K, V>[] table;

        /** 桶数组长度。 */
        private int size;

        /**
         * size - 1，理论上可以配合位运算快速计算下标。
         * 当前实现实际在 keyIndex 中重新计算了 size - 1，这个字段暂未使用。
         */
        private int sizeMask;

        /** 当前表中所有链表节点的总数。 */
        private int used;

        /**
         * 创建指定容量的哈希表。
         * Java 不允许直接创建泛型数组，所以先创建 DictEntry[]，再进行类型转换。
         * 这里通过 SuppressWarnings 压制该转换产生的 unchecked 警告。
         */
        @SuppressWarnings("unchecked")
        DictHashTable(int size) {
            this.table = (DictEntry<K, V>[]) new DictEntry[size];
            this.size = size;
            this.sizeMask = size - 1;
            this.used = 0;
        }
    }

    /**
     * 创建一个空字典。
     * 初始只创建 ht0。ht1 在第一次触发扩容时才创建，rehashIndex 使用 -1
     * 表示当前没有正在进行的 rehash。
     */
    public Dict(){
        ht0 = new DictHashTable(INITAL_SIZE);
        ht1 = null;
        rehashIndex = -1;
    }

    /**
     * 计算 key 的哈希值。
     * null 被映射为 0，虽然对外的 put 方法禁止 null key，find 方法仍保留
     * 这个防御性处理。非 null key 先取得 hashCode，再将原值与右移 16 位后的值
     * 做按位与运算。
     */
    private int hash(Object key) {
        if(key == null){return 0;}
        int h = key.hashCode();
        return h & (h >>> 16);
    }

    /**
     * 根据 key 和表容量计算桶下标。
     * size 必须是 2 的幂，size - 1 才能形成连续的低位掩码。例如 size 为 4
     * 时，size - 1 为二进制 011，按位与后得到 0 到 3 之间的下标。
     */
    private int keyIndex(Object key, int size) {
        return hash(key) & (size - 1);
    }

    /**
     * 查找 key 对应的节点。
     * 如果正在 rehash，先执行一个渐进式搬迁步骤，然后依次查询 ht0 和 ht1。
     * 这是必要的，因为 rehash 尚未完成时，同一批数据可能分别存在旧表和新表中。
     *
     * @return 找到的节点；key 为 null 或不存在时返回 null
     */
    private DictEntry<K, V> find(K key) {
        if(key == null){
            return null;
        }

        // 每次查找顺便推进一点 rehash，避免扩容工作长期拖延。
        if(rehashIndex != -1){reHashStep();}

        // 先在当前 ht0 中定位桶，再沿着桶中的冲突链逐个比较 key。
        int index = keyIndex(key, ht0.size);
        DictEntry<K, V> entry = ht0.table[index];
        while (entry != null) {
            if (entry.key.equals(key)) {
                return entry;
            }
            entry = entry.next;
        }

        // rehash 期间，新数据或已迁移数据可能在 ht1 中，因此还要查询新表。
        if(rehashIndex != -1 || ht1 != null){
            index = keyIndex(key, ht1.size);
            entry = ht1.table[index];
            while (entry != null) {
                if (entry.key.equals(key)) {
                    return entry;
                }
                entry = entry.next;
            }
        }
        return null;
    }

    /**
     * 插入或更新一条键值对。
     * 返回值遵循常见 Map 约定：如果 key 原来已经存在，返回旧值；如果是新 key，
     * 返回 null。新节点插入对应桶链表的头部，时间复杂度不需要遍历到链表尾部。
     * 扩容期间，新节点应该直接写入 ht1；这样就不会把刚插入的数据继续留在
     * 即将被废弃的 ht0 中。
     */
    public V put(K key, V value){
        // 当前实现不允许 null 作为 key，value 是否为 null 则没有额外限制。
        if (key == null) {throw new IllegalArgumentException("key cannot be null");}

        // 不在 rehash 时，根据 ht0 当前负载判断是否需要启动扩容。
        if(rehashIndex == -1){
            double loadFactor = (double) ht0.used / ht0.size;
            if (loadFactor > LOAD_FACTOR) {
                startRehash(ht0.size);
            }
        }

        // 如果在 rehash，尝试把旧表的一部分桶迁移到新表。
        // 注意：当前条件写成了 != 1；按照本类的状态约定，通常应判断 != -1。
        if(rehashIndex != -1){
            reHashStep();
        }

        // 先查找旧值；find 也可能再次推进一次 rehash。
        V oldValue = null;
        DictEntry<K, V> entry = find(key);
        if (entry != null) {
            // key 已存在时只更新 value，不新增节点，也不改变字典大小。
            oldValue = entry.value;
            entry.value = value;
            return oldValue;
        }

        int index;
        if(rehashIndex != -1){
            // rehash 进行中：新节点直接写入扩容后的 ht1。
            index = keyIndex(key, ht1.size);
            DictEntry<K, V> newEntry = new DictEntry<>(key, value);

            // 头插法：新节点指向原来的链表头，再让桶指向新节点。
            newEntry.next = ht1.table[index];
            ht1.table[index] = newEntry;
            ht1.used++;
        }else {
            // 没有 rehash：直接写入当前表 ht0。
            index = keyIndex(key, ht0.size);
            DictEntry<K, V> newEntry = new DictEntry<>(key, value);
            newEntry.next = ht0.table[index];
            ht0.table[index] = newEntry;
            ht0.used++;
        }

        // 新 key 没有旧值，因此返回 null。
        return null;
    }

    /**
     * 根据 key 获取值。
     *
     * @return key 对应的 value；key 不存在时返回 null
     */
    public V get(K key){
        DictEntry<K, V> entry = find(key);
        return entry == null ? null : entry.value;
    }

    /**
     * 删除指定 key，并返回被删除的值。
     * 删除时需要维护链表连接关系：删除头节点时更新桶头，删除中间或尾部节点
     * 时让前一个节点跳过当前节点。
     *
     * @return 被删除的 value；key 为 null 或不存在时返回 null
     */
    public V remove(K key){
        if(key ==null){
            return null;
        }

        // 删除操作也尝试推进渐进式 rehash。
        // 注意：这里同样保留了原有的 != 1 条件，和 -1 空闲标记并不完全匹配。
        if(rehashIndex != 1){
            reHashStep();
        }

        // 先查找 ht0 中对应的桶。
        int index = keyIndex(key, ht0.size);
        DictEntry<K, V> entry = ht0.table[index];

        // prev 用来记录当前节点的前驱，删除非头节点时需要修改 prev.next。
        DictEntry<K, V> prev = null;
        while (entry != null) {
            if (entry.key.equals(key)) {
                if (prev == null) {
                    // 当前节点就是桶头，直接把桶头改成下一个节点。
                    ht0.table[index] = entry.next;
                } else {
                    // 当前节点位于链表中间或尾部，让前驱节点跳过它。
                    prev.next = entry.next;
                }

                // ht0 中的节点数量减少一个。
                ht0.used--;
                return entry.value;
            }
            prev = entry;
            entry = entry.next;
        }

        // ht0 没找到时，rehash 期间还需要到 ht1 中执行同样的删除逻辑。
        if(rehashIndex != -1 || ht1 != null){
            index = keyIndex(key, ht1.size);
            entry = ht1.table[index];
            prev = null;
            while (entry != null) {
                if (entry.key.equals(key)) {
                    if (prev == null) {
                        // 删除 ht1 桶头节点。
                        ht1.table[index] = entry.next;
                    } else {
                        // 删除 ht1 链表中的非头节点。
                        prev.next = entry.next;
                    }
                    ht1.used--;
                    return entry.value;
                }
                prev = entry;
                entry = entry.next;
            }
        }

        return null;
    }

    /**
     * 启动一次渐进式 rehash。
     * 这里只创建容量扩大为原来两倍的 ht1，并把迁移游标置为 0；真正的数据搬迁
     * 由后续的 reHashStep 完成。这样启动扩容本身不需要立即遍历全部旧数据。
     */
    private void startRehash(int size){
        ht1 = new DictHashTable<>(size*2);
        rehashIndex = 0;
    }

    /**
     * 执行一次渐进式 rehash。
     * 每次调用从 ht0 的 rehashIndex 桶开始，最多处理 REHASH_MAX_SIZE 个非空桶，
     * 并且最多连续跳过 MAX_EMPTY_PERCENT 个空桶。处理完的旧桶会被置为 null，表示
     * 该桶已经迁移完成。
     */
    private void reHashStep(){
        // 没有 rehash 或新表不存在时，不需要执行任何操作。
        if (rehashIndex == -1 || ht1 == null) {return;}

        // 已经连续跳过的空桶数量。
        int emptyVisited = 0;

        // 本次调用已经迁移的非空桶数量。
        int process = 0;

        while(emptyVisited < MAX_EMPTY_PERCENT && process < REHASH_MAX_SIZE && rehashIndex < ht0.size){
            if(ht0.table[rehashIndex] == null){
                // 空桶没有节点需要搬迁，只移动游标；空桶数量会限制本次扫描范围。
                rehashIndex++;
                emptyVisited++;
                continue;
            }

            // 当前桶非空，逐个取出旧链表节点并重新按照 ht1 的容量计算下标。
            DictEntry<K, V> entry = ht0.table[rehashIndex];
            while (entry != null) {
                // 先保存旧链表的后继，否则修改 entry.next 后会丢失后续节点。
                DictEntry<K, V> next = entry.next;
                int index = keyIndex(entry.key, ht1.size);

                // 把节点头插到 ht1 的目标桶中。
                entry.next = ht1.table[index];
                ht1.table[index] = entry;

                // 节点从旧表转移到新表，因此两张表的计数同步变化。
                ht1.used++;
                ht0.used--;
                entry = next;
            }

            // 当前旧桶已经完全迁移，清空旧桶并推进游标。
            ht0.table[rehashIndex] = null;
            rehashIndex++;
            process++;
        }

        // 游标越过旧表最后一个桶，说明全部数据已经迁移完成。
        if (rehashIndex >= ht0.size) {
            // 新表成为当前表，旧表对象被释放引用，rehash 回到空闲状态。
            ht0 = ht1;
            ht1 = null;
            rehashIndex = -1;
        }
    }

    /**
     * 返回所有 key 的集合。
     * rehash 期间先推进一步，然后扫描仍可能存在数据的两张表。HashSet 会自动
     * 去重，因此即使某个 key 在迁移边界上被两个表暂时同时观察到，也不会在结果中
     * 重复出现。
     */
    public Set<K> keySet(){
        Set<K> keys = new HashSet<>();

        // 先推进一次 rehash，减少旧表中尚未迁移的数据量。
        if(rehashIndex != -1){reHashStep();}

        // 扫描当前表 ht0 的所有桶和链表。
        for (int i = 0; i < ht0.size; i++) {
            DictEntry<K, V> entry = ht0.table[i];
            while (entry != null) {
                keys.add(entry.key);
                entry = entry.next;
            }
        }

        // rehash 未完成时，再扫描新表 ht1 中已经迁移或新插入的节点。
        if(rehashIndex != -1 && ht1 != null){
            for (int i = 0; i < ht1.size; i++) {
                DictEntry<K, V> entry = ht1.table[i];
                while (entry != null) {
                    keys.add(entry.key);
                    entry = entry.next;
                }
            }
        }
        return keys;
    }

    /**
     * 返回字典当前所有键值对的浅拷贝。
     * 返回的是一个新的 HashMap，调用方修改这个 Map 不会直接改变 Dict 内部的
     * 桶结构；但 key 和 value 对象本身没有复制，仍然是原对象引用。
     */
    public Map<K, V> getAll(){
        Map<K, V> map = new HashMap<>();

        // 先收集 ht0 中的所有节点。
        for(int i = 0; i < ht0.size; i++){
            DictEntry<K, V> entry = ht0.table[i];
            while (entry != null) {
                map.put(entry.key, entry.value);
                entry = entry.next;
            }
        }

        // rehash 期间也要收集 ht1，保证尚未迁移和已经迁移的数据都能被返回。
        if(rehashIndex != -1 && ht1 != null){
            for(int i = 0; i < ht1.size; i++){
                DictEntry<K, V> entry = ht1.table[i];
                while (entry != null) {
                    map.put(entry.key, entry.value);
                    entry = entry.next;
                }
            }
        }
        return map;
    }

    /**
     * 清空字典并恢复到初始状态。
     * 直接创建一张新的初始大小 ht0，同时丢弃 ht1 和当前 rehash 游标。
     */
    public void clear(){
        ht0 = new DictHashTable<>(INITAL_SIZE);
        ht1 = null;
        rehashIndex = -1;
    }

    /**
     * 返回字典中当前存储的节点总数。
     * rehash 期间节点分布在 ht0 和 ht1 两张表中，所以必须将两张表的 used
     * 相加；没有 rehash 时 ht1 为 null，按 0 计算。
     */
    public int size(){
        return ht0.used + (ht1 == null ? 0 : ht1.used);
    }
}
