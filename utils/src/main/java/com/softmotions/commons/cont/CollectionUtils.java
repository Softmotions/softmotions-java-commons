package com.softmotions.commons.cont;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Утилитный класс для коллекций.
 *
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id: CollectionUtils.java 6006 2007-10-10 08:24:58Z adam $
 */
public class CollectionUtils {


    public static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() method is not supported");
        }
    };


    /**
     * Создает synchronized wrapper для Map объекта
     * используя read-write локи, что существенно
     * повышает производительность в многопоточных системах.
     *
     * @param m Map объект
     * @return
     */
    public static <K, V> FastSynchronizedMap<K, V> fastSynchronizedMap(Map<K, V> m) {
        return new FastSynchronizedMap<K, V>(m, new ReentrantReadWriteLock());
    }

    public static <V> FastSynchronizedCollection<V> fastSynchronizedCollection(Collection<V> coll) {
        return new FastSynchronizedCollection(coll, new ReentrantReadWriteLock());
    }

    public static <V> FastSynchronizedSet<V> fastSynchronizedSet(Set<V> set) {
        return new FastSynchronizedSet(set, new ReentrantReadWriteLock());
    }

    public static <V> FastSynchronizedList<V> fastSynchronizedList(List<V> list) {
        return new FastSynchronizedList(list, new ReentrantReadWriteLock());
    }

    private CollectionUtils() {
    }

    /**
     * Synchronized Wrapper для
     */
    public static final class FastSynchronizedMap<K, V> implements Map<K, V> {

        private final ReentrantReadWriteLock lock;

        private final Map<K, V> map;

        private FastSynchronizedMap(Map<K, V> map, ReentrantReadWriteLock lock) {
            this.map = map;
            this.lock = lock;
        }

        public ReentrantReadWriteLock getLock() {
            return lock;
        }

        public int size() {
            lock.readLock().lock();
            try {
                return map.size();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean isEmpty() {
            lock.readLock().lock();
            try {
                return map.isEmpty();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean containsKey(Object key) {
            lock.readLock().lock();
            try {
                return map.containsKey(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean containsValue(Object value) {
            lock.readLock().lock();
            try {
                return map.containsValue(value);
            } finally {
                lock.readLock().unlock();
            }
        }

        public V get(Object key) {
            lock.readLock().lock();
            try {
                return map.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        public V put(K key, V value) {
            lock.writeLock().lock();
            try {
                return map.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public V remove(Object key) {
            lock.writeLock().lock();
            try {
                return map.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void putAll(Map<? extends K, ? extends V> m) {
            lock.writeLock().lock();
            try {
                map.putAll(m);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                map.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public Set<K> keySet() {
            lock.readLock().lock();
            try {
                return new FastSynchronizedSet(map.keySet(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public Collection<V> values() {
            lock.readLock().lock();
            try {
                return new FastSynchronizedCollection(map.values(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public Set<Entry<K, V>> entrySet() {
            lock.readLock().lock();
            try {
                return new FastSynchronizedSet(map.entrySet(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public int hashCode() {
            lock.readLock().lock();
            try {
                return map.hashCode();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean equals(Object obj) {
            lock.readLock().lock();
            try {
                return map.equals(obj);
            } finally {
                lock.readLock().unlock();
            }
        }

        public String toString() {
            lock.readLock().lock();
            try {
                return map.toString();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    public static class FastSynchronizedCollection<V> implements Collection<V> {

        protected final ReentrantReadWriteLock lock;

        protected final Collection<V> coll;

        protected FastSynchronizedCollection(Collection<V> coll, ReentrantReadWriteLock lock) {
            this.coll = coll;
            this.lock = lock;
        }

        public ReentrantReadWriteLock getLock() {
            return lock;
        }

        public int size() {
            lock.readLock().lock();
            try {
                return coll.size();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean isEmpty() {
            lock.readLock().lock();
            try {
                return coll.isEmpty();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean contains(Object o) {
            lock.readLock().lock();
            try {
                return coll.contains(o);
            } finally {
                lock.readLock().unlock();
            }
        }

        public Iterator<V> iterator() {
            lock.readLock().lock();
            try {
                return new FastSynchronizedIterator(coll.iterator(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public Object[] toArray() {
            lock.readLock().lock();
            try {
                return coll.toArray();
            } finally {
                lock.readLock().unlock();
            }
        }

        public <T> T[] toArray(T[] a) {
            lock.readLock().lock();
            try {
                return coll.toArray(a);
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean add(V v) {
            lock.writeLock().lock();
            try {
                return coll.add(v);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean remove(Object o) {
            lock.writeLock().lock();
            try {
                return coll.remove(o);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean containsAll(Collection<?> c) {
            lock.readLock().lock();
            try {
                return coll.containsAll(c);
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean addAll(Collection<? extends V> c) {
            lock.writeLock().lock();
            try {
                return coll.addAll(c);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean removeAll(Collection<?> c) {
            lock.writeLock().lock();
            try {
                return coll.removeAll(c);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean retainAll(Collection<?> c) {
            lock.readLock().lock();
            try {
                return coll.retainAll(c);
            } finally {
                lock.readLock().unlock();
            }
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                coll.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int hashCode() {
            lock.readLock().lock();
            try {
                return coll.hashCode();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean equals(Object obj) {
            lock.readLock().lock();
            try {
                return coll.equals(obj);
            } finally {
                lock.readLock().unlock();
            }
        }

        public String toString() {
            lock.readLock().lock();
            try {
                return coll.toString();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    public static class FastSynchronizedList<V> extends FastSynchronizedCollection<V> implements List<V> {

        protected FastSynchronizedList(List<V> list, ReentrantReadWriteLock lock) {
            super(list, lock);
        }


        private List<V> l() {
            return (List) coll;
        }

        public boolean addAll(int index, Collection c) {
            lock.writeLock().lock();
            try {
                return l().addAll(index, c);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public V get(int index) {
            lock.readLock().lock();
            try {
                return l().get(index);
            } finally {
                lock.readLock().unlock();
            }
        }

        public V set(int index, V element) {
            lock.writeLock().lock();
            try {
                return l().set(index, element);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void add(int index, V element) {
            lock.readLock().lock();
            try {
                l().add(index, element);
            } finally {
                lock.readLock().unlock();
            }
        }

        public V remove(int index) {
            lock.writeLock().lock();
            try {
                return l().remove(index);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int indexOf(Object o) {
            lock.readLock().lock();
            try {
                return l().indexOf(o);
            } finally {
                lock.readLock().unlock();
            }
        }

        public int lastIndexOf(Object o) {
            lock.readLock().lock();
            try {
                return l().lastIndexOf(o);
            } finally {
                lock.readLock().unlock();
            }
        }

        public ListIterator listIterator() {
            lock.readLock().lock();
            try {
                return new FastSynchronizedListIterator(l().listIterator(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public ListIterator listIterator(int index) {
            lock.readLock().lock();
            try {
                return new FastSynchronizedListIterator(l().listIterator(index), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        public List subList(int fromIndex, int toIndex) {
            lock.readLock().lock();
            try {
                return new FastSynchronizedList(l().subList(fromIndex, toIndex), lock);
            } finally {
                lock.readLock().unlock();
            }
        }
    }


    public static class FastSynchronizedSet<V> extends FastSynchronizedCollection<V> implements Set<V> {

        protected FastSynchronizedSet(Collection<V> coll, ReentrantReadWriteLock lock) {
            super(coll, lock);
        }
    }

    public static class FastSynchronizedIterator<V> implements Iterator<V> {

        protected final ReentrantReadWriteLock lock;

        protected final Iterator<V> it;

        protected FastSynchronizedIterator(Iterator<V> it, ReentrantReadWriteLock lock) {
            this.it = it;
            this.lock = lock;
        }

        public ReentrantReadWriteLock getLock() {
            return lock;
        }

        public boolean hasNext() {
            lock.readLock().lock();
            try {
                return it.hasNext();
            } finally {
                lock.readLock().unlock();
            }
        }

        public V next() {
            lock.readLock().lock();
            try {
                return it.next();
            } finally {
                lock.readLock().unlock();
            }
        }

        public void remove() {
            lock.writeLock().lock();
            try {
                it.remove();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int hashCode() {
            lock.readLock().lock();
            try {
                return it.hashCode();
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean equals(Object obj) {
            lock.readLock().lock();
            try {
                return it.equals(obj);
            } finally {
                lock.readLock().unlock();
            }
        }

        public String toString() {
            lock.readLock().lock();
            try {
                return it.toString();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    public static class FastSynchronizedListIterator<V> extends FastSynchronizedIterator<V> implements ListIterator<V> {

        protected FastSynchronizedListIterator(ListIterator<V> it, ReentrantReadWriteLock lock) {
            super(it, lock);
        }

        private ListIterator<V> l() {
            return (ListIterator) it;
        }


        public boolean hasPrevious() {
            lock.readLock().lock();
            try {
                return l().hasPrevious();
            } finally {
                lock.readLock().unlock();
            }
        }

        public V previous() {
            lock.readLock().lock();
            try {
                return l().previous();
            } finally {
                lock.readLock().unlock();
            }
        }

        public int nextIndex() {
            lock.readLock().lock();
            try {
                return l().nextIndex();
            } finally {
                lock.readLock().unlock();
            }
        }

        public int previousIndex() {
            lock.readLock().lock();
            try {
                return l().previousIndex();
            } finally {
                lock.readLock().unlock();
            }
        }

        public void set(V o) {
            lock.writeLock().lock();
            try {
                l().set(o);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void add(V o) {
            lock.writeLock().lock();
            try {
                l().add(o);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
