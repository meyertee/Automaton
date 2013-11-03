package com.athaydes.automaton

import com.athaydes.automaton.mixins.SwingTestHelper
import groovy.swing.SwingBuilder
import spock.lang.Specification

import javax.swing.*
import java.awt.*

/**
 *
 * User: Renato
 */
@Mixin( SwingTestHelper )
class SwingUtilTest extends Specification implements HasSwingCode {

	JFrame jFrame

	JFrame getJFrame( ) { jFrame }

	def testNavigateBreadthFirstComponentsWholeTree( ) {
		given:
		def empty = { name -> [ getComponents: { [ ] as Component[] }, toString: { name } ] as Container }
		def c1a = empty 'c1a'
		def c2_1a = empty 'c2_1a'
		def c2_1b = empty 'c2_1b'
		def c2cp1 = empty 'c2cp1'
		def c2cp = [ getComponents: { [ c2cp1 ] as Component[] }, toString: { 'c2cp' } ] as Container
		def c2_1 = [ getMenuComponents: { [ c2_1a, c2_1b ] as Component[] }, toString: { 'c2_1' } ] as Container
		def c1 = [ getComponents: { [ c1a ] as Component[] }, toString: { 'c1' } ] as Container
		def c2 = [
				getContentPane: { c2cp }, getComponents: { [ c2_1 ] as Component[] }, toString: { 'c2' }
		] as Container
		def root = [ getComponents: { [ c1, c2 ] as Component[] }, toString: { 'root' } ] as Container

		and:
		def visited = [ ]
		def action = { Component c -> visited += c; false }

		when:
		def res = SwingUtil.navigateBreadthFirst root, action

		then:
		visited == [ root, c1, c2, c1a, c2_1, c2cp, c2_1a, c2_1b, c2cp1 ]
		!res // action never returned true
	}

	def testNavigateBreadthFirstComponentsPartialTree( ) {
		given:
		def empty = { name -> [ getComponents: { [ ] as Component[] }, toString: { name } ] as Container }
		def c1 = empty 'c1'
		def c2 = empty 'c2'
		def root = [ getComponents: { [ c1, c2 ] as Component[] }, toString: { 'root' } ] as Container

		and:
		def visited = [ ]
		def action = { Component c -> visited += c; c.toString() == 'c1' }

		when:
		def res = SwingUtil.navigateBreadthFirst root, action

		then:
		visited == [ root, c1 ]
		res
	}

	def testNavigateBreadthFirstJTreesWholeTree( ) {
		given:
		JTree mTree = null
		new SwingBuilder().edt {
			frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: false ) {
				mTree = tree( rootVisible: false )
			}
		}
		sleep 100

		and:
		def visited = [ ]

		when:
		def res = SwingUtil.navigateBreadthFirst( mTree ) {
			visited << it
			false
		}

		then:
		visited.collect { it as String } == [ mTree.model.root as String,
				'colors', 'sports', 'food',
				'blue', 'violet', 'red', 'yellow',
				'basketball', 'soccer', 'football', 'hockey',
				'hot dogs', 'pizza', 'ravioli', 'bananas' ]
		!res // action never returned true
	}

	def testNavigateBreadthFirstJTreesPartialTree( ) {
		given:
		JTree mTree = null
		new SwingBuilder().edt {
			frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: false ) {
				mTree = tree( rootVisible: false )
			}
		}
		sleep 100

		and:
		def visited = [ ]

		when:
		def res = SwingUtil.navigateBreadthFirst( mTree ) {
			visited << it
			it.toString() == 'blue'
		}

		then:
		visited.collect { it as String } == [ mTree.model.root as String,
				'colors', 'sports', 'food', 'blue' ]
		res
	}

	def testNavigateBreadthFirstJTableWholeTree( ) {
		given:
		def tModel = [
				[ firstCol: 'item 1 - Col 1', secCol: 'item 1 - Col 2' ],
				[ firstCol: 'item 2 - Col 1', secCol: 'item 2 - Col 2' ],
				[ firstCol: 'item 3 - Col 1', secCol: 'item 3 - Col 2' ]
		]

		and:
		JTable jTable = null
		new SwingBuilder().edt {
			jFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension,
					location: [ 150, 50 ] as Point, show: false ) {
				scrollPane {
					jTable = table {
						tableModel( list: tModel ) {
							propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
							propertyColumn( header: 'Col 2', propertyName: 'secCol' )
						}
					}
				}
			}
		}
		sleep 100

		and:
		def visited = [ ]

		when:
		def res = SwingUtil.navigateBreadthFirst( jTable ) { item, row, col ->
			visited << [ item, row, col ]
			false
		}

		then:
		visited == [
				[ 'Col 1', -1, 0 ], [ 'Col 2', -1, 1 ],
				[ 'item 1 - Col 1', 0, 0 ], [ 'item 1 - Col 2', 0, 1 ],
				[ 'item 2 - Col 1', 1, 0 ], [ 'item 2 - Col 2', 1, 1 ],
				[ 'item 3 - Col 1', 2, 0 ], [ 'item 3 - Col 2', 2, 1 ]
		]
		!res // action never returned true
	}

	def testLookup( ) {
		given:
		JTree mboxTree = null
		def pane1 = null, pane1_1 = null, pane1_2 = null,
		    pane1_2a = null, pane1_2b = null, menuButton = null,
		    itemExit = null

		and:
		new SwingBuilder().edt {
			jFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: false ) {
				menuBar() {
					menuButton = menu( name: 'menu-button', text: "File", mnemonic: 'F' ) {
						itemExit = menuItem( name: 'item-exit', text: "Exit", mnemonic: 'X', actionPerformed: { dispose() } )
					}
				}
				pane1 = splitPane( name: 'pane1' ) {
					pane1_1 = scrollPane( name: 'pane1-1', constraints: "left",
							preferredSize: [ 160, -1 ] as Dimension ) {
						mboxTree = tree( name: 'mboxTree', rootVisible: false )
					}
					pane1_2 = splitPane( name: 'pane1-2', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 180 ) {
						pane1_2a = scrollPane( name: 'pane1-2a', constraints: "top" ) { table() }
						pane1_2b = scrollPane( name: 'pane1-2b', constraints: "bottom" ) { textArea() }
					}
				}
			}
		}

		sleep 100

		expect:
		menuButton == SwingUtil.lookup( 'menu-button', jFrame )
		itemExit == SwingUtil.lookup( 'item-exit', jFrame )
		mboxTree == SwingUtil.lookup( 'mboxTree', jFrame )
		pane1 == SwingUtil.lookup( 'pane1', jFrame )
		pane1_1 == SwingUtil.lookup( 'pane1-1', pane1 )
		pane1_2 == SwingUtil.lookup( 'pane1-2', pane1 )
		pane1_2a == SwingUtil.lookup( 'pane1-2a', jFrame )
		pane1_2b == SwingUtil.lookup( 'pane1-2b', pane1_2 )
	}

	def testCallMethodIfExists( ) {
		when:
		def result = SwingUtil.callMethodIfExists( obj, methodName, args )

		then:
		result == expected

		where:
		obj             | methodName          | args            | expected
		'hi'            | 'toUpperCase'       | [ ] as Object[] | 'HI'
		'hi'            | 'nonExistentMethod' | [ ] as Object[] | [ ]
		[ 1 ]           | 'add'               | 2               | true
		[ 1, 2 ]        | 'addAll'            | [ 3, 4 ]        | true
		[ 1, 2 ] as Set | 'addAll'            | [ 1, 2 ]        | false
	}

	def testText( ) {
		given:
		def tModel = [ [ firstCol: 'item 1 - Col 1', secCol: 'item 1 - Col 2' ] ]
		new SwingBuilder().edt {
			jFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: true ) {
				menuBar() {
					menu( text: "File", mnemonic: 'F' ) {
						menuItem( text: "Exit", mnemonic: 'X', actionPerformed: { dispose() } )
					}
				}
				splitPane {
					scrollPane( constraints: "left", minimumSize: [ 150, -1 ] as Dimension ) {
						panel() {
							label( text: 'A tree' )
							tree( rootVisible: false )
							table {
								tableModel( list: tModel ) {
									propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
									propertyColumn( header: 'Col 2', propertyName: 'secCol' )
								}
							}
						}
					}
					splitPane( orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 180 ) {
						scrollPane( constraints: "top" ) { button( text: 'Click' ) }
						scrollPane( constraints: "bottom" ) { textArea() }
					}
				}
			}
		}

		waitForJFrameToShowUp()

		expect:
		SwingUtil.text( textToFind, jFrame ) != null

		cleanup:
		jFrame?.dispose()

		where:
		textToFind << [ 'File', 'Exit', 'A tree', 'Click', 'colors', 'Col 1', 'item 1 - Col 1' ]
	}

	def testFakeComponentForTreeNode( ) {
		given:
		def bounds = new Rectangle( 5, 6, 7, 8 )
		def parentAbsLocation = new Point( 20, 30 )

		when:
		Component component = SwingUtil.fakeComponentFor( parentAbsLocation, bounds )

		then:
		component.locationOnScreen == new Point( 25, 36 )
		component.width == 7
		component.height == 8
	}

}
