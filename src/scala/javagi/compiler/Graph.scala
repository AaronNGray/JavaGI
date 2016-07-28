package javagi.compiler

import scala.collection.immutable.Map
import scala.collection.mutable.{HashMap, Buffer, ArrayBuffer}

object Graph {

  def apply[Vertex](edges: Iterable[(Vertex, Vertex)]): Graph[Vertex,Unit] = {
    var vertices = Set[Vertex]()
    for ((x, y) <- edges) {
      vertices = vertices + x + y
    }
    apply(vertices, for ((v1,v2) <- edges) yield (v1, (), v2))
  }

  def apply2[Vertex](vertices: Iterable[Vertex], edges: Iterable[(Vertex, Vertex)]): Graph[Vertex,Unit] = {
    apply(vertices, for ((v1,v2) <- edges) yield (v1, (), v2))
  }

  def apply[Vertex, Label](vertices: Iterable[Vertex], edges: Iterable[(Vertex, Label, Vertex)]): Graph[Vertex,Label] = {
    type Edge = (Vertex, Label, Vertex)
    type GMap = Map[Vertex, List[(Vertex, Label)]]
    if (! edges.forall((e: Edge) => vertices.exists(_ == e._1) && vertices.exists(_ == e._3)))
      GILog.bug("Graph.apply: not all vertices present in argument 'vertices'")
    else {
      val emptyGraph = vertices.foldLeft(Map() : GMap)((m: GMap, v: Vertex) => m + ((v,Nil)))
      val m = edges.foldLeft(emptyGraph)((m: GMap, e: Edge) => m.update(e._1, (e._3, e._2) :: m.getOrElse(e._1, Nil)))
      new Graph(m)
    }
  }
}

class Graph[Vertex,Label] private (val map: Map[Vertex, List[(Vertex, Label)]]) {
  
  type Edge = (Vertex, Label, Vertex)
  type GMap = Map[Vertex, List[(Vertex, Label)]]
  type Path = List[Edge]

  override val toString = map.toString

  /*
   * Algorithm from the paper "A new way to enumerate cycles in graph" by Liu et al.
   *  (AICT/ICIW 2006) 
   */
  def enumAllCycles(): List[Path] = {
    val measure = Map(map.keySet.toList.zipWithIndex : _*)
    
    val selfLoops: Iterable[Path] =
      map.keySet.flatMap((v: Vertex) => 
                          Utils.mapOption(map(v), (p: (Vertex, Label)) => 
                                                  if (p._1 == v) Some(List((v, p._2, p._1))) else None))
    
    val initialQueue: Iterable[(Path, Vertex)] =
      map.keySet.flatMap((head: Vertex) => 
                          Utils.mapOption(map(head), (p: (Vertex, Label)) => 
                                                     if (measure(p._1) > measure(head)) 
                                                       Some((List((head, p._2, p._1)), head)) 
                                                     else None))
    
    def pathElem(v: Vertex, p: Path) = 
      p.find((e: Edge) => v == e._1 || v == e._3).isDefined
    
    def newOpenPaths(rpath: Path, head: Vertex, tail: Vertex): List[(Path, Vertex)] = {
      Utils.mapOption(map(tail), (p: (Vertex,Label)) =>
                                 if (! (pathElem(p._1, rpath)) && measure(p._1) > measure(head))
                                   Some(((tail,p._2,p._1) :: rpath, head))
                                 else None)
                                     
    }
    
    def loop(queue: List[(Path, Vertex)], /* The queue. Every path of the queue is in reverse
                                             order; that is, the real path can be obtained
                                             by reversing the list representing the path.
                                             Paths are never empty.
                                             The 2nd component of every queue element
                                             is the head of the path stored in the 1st component. */
             acc: List[Path]): List[Path] = queue match {
      case Nil => acc
      case ((rpath,head) :: rest) => {
        val (_,_,tail) :: _ = rpath
        val rest_ = newOpenPaths(rpath, head, tail) ++ rest
        map(tail).find( (p: (Vertex,Label)) => p._1 == head ) match {
          case None => loop(rest_, acc)
          case Some((_,l)) => loop(rest_, ((tail,l,head) :: rpath).reverse :: acc)
        }
      }
    }
    
    selfLoops.toList ++ loop(initialQueue.toList, Nil)
  }

  def hasCycle(): Boolean = {
    // FIXME
    // not running fast but fast to implement ;-)
    ! enumAllCycles.isEmpty
  }

  def topsort(): List[Vertex] = {
    val white = 0
    val gray = 1
    val black = 2
    val colors: HashMap[Vertex, Int] = new HashMap()
    val vertices = map.keySet
    val buf: Buffer[Vertex] = new ArrayBuffer()
    def dfsVisit(v: Vertex): Unit = {
      colors.put(v, gray)
      val targets = map.getOrElse(v, Nil)
      for ((t,_) <- targets) {
        val c = colors.get(t)
        if (c == Some(white)) dfsVisit(t)
        else if (c == Some(gray)) GILog.bug("cycle in graph")
      }
      colors.put(v, black)
      buf.insert(0, v)
    }
    for (v <- vertices) {
      colors.put(v, white)
    }
    for (v <- vertices) {
      if (colors.get(v) == Some(white)) dfsVisit(v)
    }
    buf.toList
  }
}
