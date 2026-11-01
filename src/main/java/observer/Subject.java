package observer;

public interface Subject<T> {
    void subscribe(EventType type, Observer<T> observer);
    void unsubscribe(EventType type, Observer<T> observer);
    void notifyListeners(T event);
}
