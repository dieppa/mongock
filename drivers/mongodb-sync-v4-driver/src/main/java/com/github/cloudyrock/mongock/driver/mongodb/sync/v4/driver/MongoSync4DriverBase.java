package com.github.cloudyrock.mongock.driver.mongodb.sync.v4.driver;

import com.github.cloudyrock.mongock.MongockConnectionDriver;
import com.github.cloudyrock.mongock.driver.mongodb.sync.v4.decorator.impl.MongoDataBaseDecoratorImpl;
import com.github.cloudyrock.mongock.driver.mongodb.sync.v4.repository.MongoSync4LockRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.changock.driver.api.driver.ChangeSetDependency;
import io.changock.driver.api.entry.ChangeEntry;
import io.changock.driver.api.entry.ChangeEntryService;
import io.changock.driver.api.lock.guard.invoker.LockGuardInvokerImpl;
import io.changock.driver.core.driver.ConnectionDriverBase;
import io.changock.driver.core.lock.LockRepository;
import io.changock.migration.api.exception.ChangockException;
import io.changock.utils.annotation.NotThreadSafe;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;

@NotThreadSafe
public abstract class MongoSync4DriverBase<CHANGE_ENTRY extends ChangeEntry>
    extends ConnectionDriverBase<CHANGE_ENTRY>
    implements MongockConnectionDriver<CHANGE_ENTRY> {

  private static final String DEFAULT_CHANGELOG_COLLECTION_NAME = "mongockChangeLog";
  private static final String DEFAULT_LOCK_COLLECTION_NAME = "mongockLock";

  protected final MongoDatabase mongoDatabase;
  protected String changeLogCollectionName = DEFAULT_CHANGELOG_COLLECTION_NAME;
  protected String lockCollectionName = DEFAULT_LOCK_COLLECTION_NAME;
  protected boolean indexCreation = true;
  protected MongoSync4LockRepository lockRepository;
  protected Set<ChangeSetDependency> dependencies;

  protected MongoSync4DriverBase(MongoDatabase mongoDatabase,
                                 long lockAcquiredForMinutes,
                                 long maxWaitingForLockMinutes,
                                 int maxTries) {
    super(lockAcquiredForMinutes, maxWaitingForLockMinutes, maxTries);
    this.mongoDatabase = mongoDatabase;
  }

  @Override
  public void setChangeLogCollectionName(String changeLogCollectionName) {
    this.changeLogCollectionName = changeLogCollectionName;
  }

  @Override
  public void setLockCollectionName(String lockCollectionName) {
    this.lockCollectionName = lockCollectionName;
  }

  @Override
  public String getChangeLogCollectionName() {
    return changeLogCollectionName;
  }

  @Override
  public String getLockCollectionName() {
    return lockCollectionName;
  }
  @Override
  public void setIndexCreation(boolean indexCreation) {
    this.indexCreation = indexCreation;
  }

  @Override
  public void runValidation() throws ChangockException {
    if (mongoDatabase == null) {
      throw new ChangockException("MongoDatabase cannot be null");
    }
    if (this.getLockManager() == null) {
      throw new ChangockException("Internal error: Driver needs to be initialized by the runner");
    }
  }

  @Override
  protected LockRepository getLockRepository() {
    if (lockRepository == null) {
      MongoCollection<Document> collection = mongoDatabase.getCollection(lockCollectionName);
      this.lockRepository = new MongoSync4LockRepository(collection, indexCreation);
    }
    return lockRepository;
  }

  @Override
  public Set<ChangeSetDependency> getDependencies() {
    if(dependencies == null) {
      throw new ChangockException("Driver not initialized");
    }
    return dependencies;
  }

  @Override
  public void specificInitialization() {
    dependencies = new HashSet<>();
    dependencies.add(new ChangeSetDependency(MongoDatabase.class, new MongoDataBaseDecoratorImpl(mongoDatabase, new LockGuardInvokerImpl(getLockManager()))));
    dependencies.add(new ChangeSetDependency(ChangeEntryService.class, getChangeEntryService()));
  }
}
