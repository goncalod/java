/**
 * An object that maps keys to values.
 * A map cannot contain duplicate keys; each key can map to at most one value.
 * 
 * @author Gon�alo Dias
 * @date 18/12/2011
 *
 * @param <Key>
 * @param <Value>
 */



public class Dictionary<Key extends Comparable<Key>, Value> {
	
	
	private static class Node<K extends Comparable<K>, V> {
		
		K 				key;
		V 				value;
		Node<K,V>[]  	next;
		
		@SuppressWarnings("unchecked")
		public Node(K k, V v, int length) {
			key = k;
			value = v;
			next = new Node[length];
		}		
	}
	
	private static class NodeStatus<Key extends Comparable<Key>, Value> 
	{
		boolean canBePerformed;
		int levelLinked;
		int levels;
		
		public NodeStatus(boolean canBePerformed) {
			this.canBePerformed = canBePerformed;	
			this.levelLinked = -1;
		}
		
		public NodeStatus(boolean canBePerformed, int levels) {
			this(canBePerformed);
			this.levels = levels;
		}
	}
	
	//
	// Private Fields
	// 
	
	private static final int default_num_lists = 16;
	
	private int[]			 _levelsNodes;
	private Node<Key, Value> _lists;
	private int 			 _currentLevel;
	
	
	//
	// Constructors
	// 
	
	private void init(int numLists) 
	{
		_lists = new Node<>(null, null, numLists);	// null key & null value (dummy)
		_levelsNodes = new int[numLists];
		_currentLevel = 1;
	}
	
	public Dictionary() 
	{
		init(default_num_lists);
	}
	
	public Dictionary(int numLists) 
	{
		if(numLists <= 0) 
			numLists = default_num_lists;
		
		init(numLists);
	}
	
	
	//
	// Private auxiliary methods
	//
	
	//
	// Generate in a pseudo-random way a level from 1 to maxLevel
	//
	private static int newLevel(int maxLevel) 
	{		
		int level, j;
		double t = Math.random();
		
		for(level = 1, j = 2;  level < maxLevel;  ++level, j+=j)
			if( t*j > 1.0 ) break;
		
		return level;
	}
	
	
	//
	// Public interface
	//
	
	public boolean isEmpty() {
		
		// O(1) Operation
		
		return _lists.next[0] == null;
	}
	
	
	// 
	// Do a binary search in a recursive mode
	//
	private Value _searchR(Key k, int level, Node<Key, Value> currNode)
	{
		
		//
		// Stop conditions
		//
		
		if( currNode.key != null) {
		
			// Key found
			if( k.compareTo(currNode.key) == 0 ) return currNode.value;
						
			// Between the call we passed to a higher key than the key we are searching.
			if( level == 0  &&  k.compareTo(currNode.key) < 0 ) 
				return null;
			
			// This is the last node and the key we are searching is higher than actual
			if( currNode.next[0] == null  &&  k.compareTo(currNode.key) > 0 ) 
				return null;			
		}
					
		
		//
		// Process for each node
		// 
		
		// Until we don't get a valid node (!= NULL) we decrement the level.
		for(  ; level > 0  &&  currNode.next[level] == null; level--);
		
		//
		// After this, all nodes down are != NULL.
		//
		
		// While k is lower than next node we decrement the level. 
		// (If is lower means than the key searching is between currNode and currNode.next[level])
		for(  ; level > 0  &&  k.compareTo(currNode.next[level].key) < 0; level--);
		
		
		// Repeat the process for the next node	recursively
		return _searchR(k, level, currNode.next[level]);		
	}
	
	/**
	 * Search on the dictionary by the specified key.
	 * @param key
	 * @return null if not found, or the value if found.
	 */
	public Value search(Key key) 
	{
		if( isEmpty() )	
			return null;
		
		return _searchR(key, _currentLevel - 1, _lists);		
	}	

	
	
	//
	// Try connect currNode next references to newNode node.
	// It will try until we reached the length of the newNode or we reached the length of the currNode.
	// For each connect, this method increments the field of status instance levelLinked by one unit.
	// 
	private void tryConnect(NodeStatus<Key, Value> status, Node<Key, Value> currNode, Node<Key, Value> newNode) {
		
		for(int i = status.levelLinked + 1;  i < currNode.next.length && i < newNode.next.length;  i++) {
			
			newNode.next[i] = currNode.next[i];
			currNode.next[i] = newNode;
			
			++status.levelLinked;
		}			
	}
	
	//
	// Do a binary search in a recursive mode, and establish the connections as recursion is going back
	// 
	private NodeStatus<Key, Value> _insertR(
			Node<Key, Value> prevNode,
			Node<Key, Value> currNode, 
			int level, 
			Node<Key, Value> newNode)
	{
	
		if( currNode.key != null ) 
		{			
			if( newNode.key.compareTo(currNode.key) == 0 )
				return new NodeStatus<>(false);	// duplicated key!
				
			if( currNode.next[0] == null && newNode.key.compareTo(currNode.key) > 0) {
				
				//
				// Last node and we want insert at the end.
				// We add the node at front of currentNode
				//
				
				NodeStatus<Key, Value> s = new NodeStatus<>(true, newNode.next.length - 1);		
				tryConnect(s, currNode, newNode);		
				return s;		
			}
				
			if( level == 0 && newNode.key.compareTo(currNode.key) < 0 ) {
				
				//
				// We add the node at front of prevNode
				//
				
				NodeStatus<Key, Value> s = new NodeStatus<>(true, newNode.next.length - 1);		
				tryConnect(s, prevNode, newNode);		
				return s;		
			}
		}
		
		for(  ; level > 0  &&  currNode.next[level] == null; level-- );
		
		for(  ; level > 0  &&  newNode.key.compareTo(currNode.next[level].key) < 0; level--);
		
		// Try insert
		NodeStatus<Key, Value> status = _insertR(currNode, currNode.next[level], level, newNode);
		
		// We cannot insert, so we return immediately
		if( !status.canBePerformed )
			return status;
		
		//
		// We can insert and we need to fix-up relationships until all level links are linked on the newNode
		// 
		
		if( status.levelLinked < status.levels ) {
			tryConnect(status, currNode, newNode);
		}
		
		return status;		
	}

	/**
	 * Try insert the key with corresponding value on the dictionary.
	 * @param key
	 * @param value
	 * @param levelForDebug
	 * @return false if key is null or if the key is already on the dictionary or true if sucessfully inserted.
	 */
	public boolean insert(Key key, Value value) 
	{
		if(key == null) 
			return false;
		
		//
		// We cannot use search algorithm here because we must establish the
		// connections while recursion is going back!
		// 
		
		
		int level = newLevel(_lists.next.length - 1); 	// Set level
	
		Node<Key, Value> newNode = new Node<>(key, value, level);
		NodeStatus<Key, Value> status;
		
		if( isEmpty() ) {
			
			//
			// Set lists node next links to the new node
			// 
			
			for(int i = 0; i < level; _lists.next[i] = newNode, i++);
			status = new NodeStatus<>(true);
		}
		
		else {
			
			int l = _currentLevel - 1;
			status = _insertR(null, _lists, l, newNode);
			
		}
		
		
		if( !status.canBePerformed )
			return false;	// This key is already on dictionary and cannot be duplicated!
		
		//
		// We update the currentLevel if the generated value is higher than actual.
		// 
		
		if( level > _currentLevel )
			_currentLevel = level;
		
		_levelsNodes[level - 1]++; // Increment the count value of the nodes with that level.
		return true;
	}
	
	
	
	
	//
	// Disconnect nodeToRemove from prevNode below level.
	//
	private void removeInternal(Node<Key, Value> prevNode, Node<Key, Value> nodeToRemove, int level) {
		
		Node<Key, Value> current = prevNode;
		Key k = nodeToRemove.key;
		
		do {
			
			if( k.compareTo(current.next[level].key) == 0 ) {
				
				current.next[level] = nodeToRemove.next[level];				
				if( --level < 0 ) break;				
			}	
			
			//
			// Only if the next node is not the node to remove we can advance to it!
			// 
			
			if( current.next[level] != nodeToRemove )
				current = current.next[level];
		} 
		
		while(true);
	}
	
	
	//
	// Do a binary search in a recursive mode and if the key is found invoke removeInternal to discard the node that contain that key.
	//
	private NodeStatus<Key, Value> _removeR(
			Node<Key, Value> prevNode,
			Node<Key, Value> currNode, 
			int level, 
			Key k)
	{				
		if ( currNode.key != null ) 
		{
			if( currNode.next[0] == null && k.compareTo(currNode.key) > 0) 
				return new NodeStatus<>(false);
				
			if( level == 0 && k.compareTo(currNode.key) < 0 ) 
				return new NodeStatus<>(false);
			
			if( currNode.key.compareTo(k) == 0 ) {
				removeInternal(prevNode, currNode, level);
				return new NodeStatus<>(true, currNode.next.length - 1);
			}
		}
		
		
		for(  ; level > 0  &&  currNode.next[level] == null; level-- );
		
		for(  ; level > 0  &&  k.compareTo(currNode.next[level].key) < 0; level--);
		
		// Try remove
		return _removeR(currNode, currNode.next[level], level, k);	
	}

	/**
	 * Remove the specified key from dictionary.
	 * @param key
	 * @return false if key is null or not found or true if successfully removed.
	 */
	public boolean remove(Key key) {
		
		if( key == null ) 
			return false;
		
		if( isEmpty() ) 
			return false;
		
		int l = _currentLevel - 1;
		NodeStatus<Key, Value> status = _removeR(null, _lists, l, key);
		
		if( !status.canBePerformed )
			return false;		// Key wasn't found!
		
		//
		// Already performed (removed)
		//
		
		int nodeLevel = status.levels;
		
		if( --_levelsNodes[nodeLevel] == 0  && (nodeLevel + 1) == _currentLevel){
			
			//
			// If i am the lastNode and i am the currentLevel, i need to find 
			// a level with nodes while updating the _currentLevel
			//
			_currentLevel--;
			for(  ; --nodeLevel > 0 && _levelsNodes[nodeLevel] == 0; _currentLevel-- ); 				
		}			
		
		return true;		
	}
}
