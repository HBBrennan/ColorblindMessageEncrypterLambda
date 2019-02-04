package ColorblindMessageEncrypter;

import lombok.*;

import java.awt.*;
import java.util.ArrayList;

/**
 *  Quadtree class for integer coordinates
 * @param <Value> The inserted value
 */
@NoArgsConstructor
public class PointQuadTree<Value>  {
	private PointQuadTreeNode root;

	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	public class PointQuadTreeNode {
		@NonNull int x, y;     // x- and y- coordinates
		PointQuadTreeNode NW, NE, SE, SW;   // four subtrees
		@NonNull Value value;  // associated data
	}

	public void insert(int x, int y, Value value) {
		root = insert(root, x, y, value);
	}

	private PointQuadTreeNode insert(PointQuadTreeNode h, int x, int y, Value value) {
		if (h == null) return new PointQuadTreeNode(x, y, value);
		else if ((x == h.x) && (y == h.y)) h.value = value;  // duplicate
		else if ((x < h.x) && (y < h.y)) h.SW = insert(h.SW, x, y, value);
		else if ((x < h.x)) h.NW = insert(h.NW, x, y, value);
		else if ((y < h.y)) h.SE = insert(h.SE, x, y, value);
		else h.NE = insert(h.NE, x, y, value);
		return h;
	}

	public ArrayList<PointQuadTreeNode> query2D(Rectangle rect) {
		val ret = new ArrayList<PointQuadTreeNode>();
		query2D(root, rect, ret);
		return ret;
	}

	private void query2D(PointQuadTreeNode h, Rectangle rect, ArrayList<PointQuadTreeNode> ret) {
		if (h == null) return;
		int xMax = rect.x + rect.width;
		int yMax = rect.y + rect.height;
		if (rect.contains(h.x, h.y))
			ret.add(h);
		if ((rect.x < h.x) &&  (rect.y < h.y)) query2D(h.SW, rect, ret);
		if ((rect.x < h.x) && !(yMax < h.y)) query2D(h.NW, rect, ret);
		if (!(xMax < h.x) &&  (rect.y < h.y)) query2D(h.SE, rect, ret);
		if (!(xMax <h.x) && !(yMax < h.y)) query2D(h.NE, rect, ret);
	}

	public ArrayList<PointQuadTreeNode> toList() {
		val ret = new ArrayList<PointQuadTreeNode>();
		toListHelper(ret, root);
		return ret;
	}

	private void toListHelper(ArrayList<PointQuadTreeNode> list, PointQuadTreeNode h) {
		if (h == null) return;
		list.add(h);
		toListHelper(list, h.NW);
		toListHelper(list, h.NE);
		toListHelper(list, h.SW);
		toListHelper(list, h.SE);
	}
}
