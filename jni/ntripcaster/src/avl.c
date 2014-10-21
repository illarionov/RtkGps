/* libavl
 * - manipulates AVL trees.
 *
 * Copyright (c) 2003
 * German Federal Agency for Cartography and Geodesy (BKG)
 *
 * Developed for Networked Transport of RTCM via Internet Protocol (NTRIP)
 * for streaming GNSS data over the Internet.
 *
 * Designed by Informatik Centrum Dortmund http://www.icd.de
 *
 * NTRIP is currently an experimental technology.
 * The BKG disclaims any liability nor responsibility to any person or entity
 * with respect to any loss or damage caused, or alleged to be caused,
 * directly or indirectly by the use and application of the NTRIP technology.
 *
 * For latest information and updates, access:
 * http://igs.ifag.de/index_ntrip.htm
 *
 * Georg Weber
 * BKG, Frankfurt, Germany, June 2003-06-13
 * E-mail: euref-ip@bkg.bund.de
 *
 * Based on the GNU General Public License published Icecast 1.3.12
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

#ifdef HAVE_CONFIG_H
#ifdef _WIN32
#include <win32config.h>
#else 
#include <config.h>
#endif
#endif

#if PSPP
#include "common.h"
#include "arena.h"
#define HAVE_XMALLOC 1
#endif
#if SELF_TEST 
#include <limits.h>
#endif

#include "definitions.h"
#include <stdio.h>

#ifdef HAVE_SIGNAL_H
#include <signal.h>
#elif HAVE_SYS_SIGNAL_H
#include <sys/signal.h>
#endif

#include <stdlib.h>
#ifdef HAVE_ASSERT_H
#include <assert.h>
#endif
#ifdef _WIN32
#include <assert.h>
#endif

#include <sys/types.h>
#ifdef HAVE_UNISTD_H
# include <unistd.h>
#endif

#include "avl.h"

#if !PSPP && !__GCC__
#define inline
#endif

#if !PSPP
#if __GNUC__ >= 2
#define unused __attribute__ ((unused))
#else
#define unused
#endif
#endif

/* avl_functions.c. ajd ************************************************/

#ifndef __USE_BSD
#define __USE_BSD
#endif
#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif

#include <string.h>
#include <time.h>

#ifndef _WIN32
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include "ntripcaster.h"
#include "utility.h"
#include "ntrip_string.h"
#include "connection.h"
#include "log.h"
#include "threads.h"
#include "client.h"
#include "sock.h"


#ifdef HAVE_XMALLOC
void *xmalloc (size_t);
#else

static void *xmalloc (size_t size)
{
	void *vp;

	if (size == 0)
		return NULL;
	vp = malloc(size);

	assert(vp != NULL);
	if (vp == NULL) {
		fprintf(stderr, "virtual memory exhausted\n");
		exit(EXIT_FAILURE);
	}
	
	return vp;
}
#endif

avl_tree *avl_create(MAYBE_ARENA avl_comparison_func cmp, void *param)
{
	avl_tree *tree;

	assert(cmp != NULL);
#if PSPP
  if (owner)
	tree = arena_alloc (owner, sizeof (avl_tree));
  else
#endif
	tree = xmalloc (sizeof (avl_tree));

#if PSPP
	tree->owner = owner;
#endif
  tree->root.link[0] = NULL;
  tree->root.link[1] = NULL; 
  tree->cmp = cmp;
  tree->count = 0;
  tree->param = param;
  thread_create_mutex(&tree->mutex)

	return tree;
}

avl_tree *avl_create_nl(MAYBE_ARENA avl_comparison_func cmp, void *param)
{
	avl_tree *tree;

	assert(cmp != NULL);
#if PSPP
  if (owner)
	tree = arena_alloc (owner, sizeof (avl_tree));
  else
#endif
	tree = xmalloc (sizeof (avl_tree));

#if PSPP
	tree->owner = owner;
#endif
  tree->root.link[0] = NULL;
  tree->root.link[1] = NULL; 
  tree->cmp = cmp;
  tree->count = 0;
  tree->param = param;
  thread_create_mutex_nl(&tree->mutex);
  return tree;
}

void avl_destroy(avl_tree *tree, avl_node_func free_func)
{
	assert(tree != NULL);
  
  internal_lock_mutex(&tree->mutex);
#if PSPP
  if (free_func || tree->owner == NULL)
#endif
    {
      avl_node *an[AVL_MAX_HEIGHT];	
      unsigned long ab = 0;
      int ap = 0;
      avl_node *p = tree->root.link[0];

      for (;;)
	{
	  /* T2. */
	  while (p != NULL)
	    {
	      /* T3. */
	      ab &= ~(1ul << ap);
	      an[ap++] = p;
	      p = p->link[0];
	    }

	  /* T4. */
	  for (;;)
	    {
	      if (ap == 0)
		goto done;

	      p = an[--ap];
	      if ((ab & (1ul << ap)) == 0)
		{
		  ab |= (1ul << ap++);
		  p = p->link[1];
		  break;
		}
      
	      if (free_func)
		free_func (p->data, tree->param);
#if PSPP
	      if (tree->owner == NULL)
#endif
		free (p);
	    }
	}
    }

 done:
	internal_unlock_mutex(&tree->mutex);
	thread_mutex_destroy(&tree->mutex);
#if PSPP
  if (tree->owner == NULL)
#endif
    free (tree);
}

int
avl_count (const avl_tree *tree)
{
  assert (tree != NULL);
  return tree->count;
}

#if PSPP
static inline avl_node *
new_node (arena **owner)
{
  if (owner != NULL)
    return arena_alloc (owner, sizeof (avl_node));
  else
    return xmalloc (sizeof (avl_node));
}
#else
static inline avl_node *
new_node (void)
{
  return xmalloc (sizeof (avl_node));
}

#define new_node(owner)				\
	new_node ()
#endif


void *
avl_traverse (avl_tree *tree, avl_traverser *trav)
{
	assert (tree && trav);

	internal_lock_mutex(&tree->mutex);


  if (trav->init == 0)
    {
      trav->init = 1;
      trav->nstack = 0;
      trav->p = tree->root.link[0];
    }
  else
    trav->p = trav->p->link[1];

  for (;;)
    {
	    while (trav->p != NULL)
	    {
		    trav->stack[trav->nstack++] = trav->p;
		    trav->p = trav->p->link[0];
	    }
	    if (trav->nstack == 0)
	    {
		    trav->init = 0;
		    internal_unlock_mutex(&tree->mutex);
		    return NULL;
	    }
	    trav->p = trav->stack[--trav->nstack];
	    
	    internal_unlock_mutex(&tree->mutex);
	    return trav->p->data;
    }
  internal_unlock_mutex(&tree->mutex);
}

void **
avl_probe (avl_tree *tree, void *item)
{
  avl_node *t;
  avl_node *s, *p, *q, *r;
  
  assert (tree != NULL);
  internal_lock_mutex(&tree->mutex);
  t = &tree->root;
  s = p = t->link[0];

  if (s == NULL)
    {
      tree->count++;
      assert (tree->count == 1);
      q = t->link[0] = new_node (tree->owner);
      q->data = item;
      q->link[0] = q->link[1] = NULL;
      q->bal = 0;
	  internal_unlock_mutex(&tree->mutex);
      return &q->data;
    }

  for (;;)
    {
      int diff = tree->cmp (item, p->data, tree->param);

      if (diff < 0)
	{
	  p->cache = 0;
	  q = p->link[0];
	  if (q == NULL)
	    {
	      p->link[0] = q = new_node (tree->owner);
	      break;
	    }
	}
      else if (diff > 0)
	{
	  p->cache = 1;
	  q = p->link[1];
	  if (q == NULL)
	    {
	      p->link[1] = q = new_node (tree->owner);
	      break;
	    }
	}
      else
	  {
	internal_unlock_mutex(&tree->mutex);
	return &p->data;
	  }

      if (q->bal != 0)
	t = p, s = q;
      p = q;
    }
  
  tree->count++;
  q->data = item;
  q->link[0] = q->link[1] = NULL;
  q->bal = 0;

  r = p = s->link[(int) s->cache];
  while (p != q)
    {
      p->bal = p->cache * 2 - 1;
      p = p->link[(int) p->cache];
    }

  if (s->cache == 0)
    {
      if (s->bal == 0)
	{
	  s->bal = -1;
	  internal_unlock_mutex(&tree->mutex);
	  return &q->data;
	}
      else if (s->bal == +1)
	{
	  s->bal = 0;
	  internal_unlock_mutex(&tree->mutex);
	  return &q->data;
	}
      
      assert (s->bal == -1);
      if (r->bal == -1)
	{
	  p = r;
	  s->link[0] = r->link[1];
	  r->link[1] = s;
	  s->bal = r->bal = 0;
	}
      else
	{
	  assert (r->bal == +1);
	  p = r->link[1];
	  r->link[1] = p->link[0];
	  p->link[0] = r;
	  s->link[0] = p->link[1];
	  p->link[1] = s;
	  if (p->bal == -1)
	    s->bal = 1, r->bal = 0;
	  else if (p->bal == 0)
	    s->bal = r->bal = 0;
	  else 
	    {
	      assert (p->bal == +1);
	      s->bal = 0, r->bal = -1;
	    }
	  p->bal = 0;
	}
    }
  else
    {
      if (s->bal == 0)
	{
	  s->bal = 1;
	  internal_unlock_mutex(&tree->mutex);
	  return &q->data;
	}
      else if (s->bal == -1)
	{
	  s->bal = 0;
	  internal_unlock_mutex(&tree->mutex);
	  return &q->data;
	}

      assert (s->bal == +1);
      if (r->bal == +1)
	{
	  p = r;
	  s->link[1] = r->link[0];
	  r->link[0] = s;
	  s->bal = r->bal = 0;
	}
      else
	{
	  assert (r->bal == -1);
	  p = r->link[0];
	  r->link[0] = p->link[1];
	  p->link[1] = r;
	  s->link[1] = p->link[0];
	  p->link[0] = s;
	  if (p->bal == +1)
	    s->bal = -1, r->bal = 0;
	  else if (p->bal == 0)
	    s->bal = r->bal = 0;
	  else 
	    {
	      assert (p->bal == -1);
	      s->bal = 0, r->bal = 1;
	    }
	  p->bal = 0;
	}
    }
		
  if (t != &tree->root && s == t->link[1])
    t->link[1] = p;
  else
    t->link[0] = p;
  
  internal_unlock_mutex(&tree->mutex);
  return &q->data;
}
  
void *
avl_find (avl_tree *tree, const void *item)
{
  const avl_node *p;
  p = NULL;

  assert (tree != NULL);
  internal_lock_mutex(&tree->mutex);
  for (p = tree->root.link[0]; p; )
    {
      int diff = tree->cmp (item, p->data, tree->param);

      if (diff < 0)
	p = p->link[0];
      else if (diff > 0)
	p = p->link[1];
      else
	  {
		  internal_unlock_mutex(&tree->mutex);
		  return p->data;
	  }
    }
  internal_unlock_mutex(&tree->mutex);
  return NULL;
}


void *
avl_delete (avl_tree *tree, const void *item)
{
  avl_node *pa[AVL_MAX_HEIGHT];	
  char a[AVL_MAX_HEIGHT];		
  int k = 1;	
  
  avl_node **q;
  avl_node *p;

  assert (tree != NULL);
  internal_lock_mutex(&tree->mutex);

  a[0] = 0;
  pa[0] = &tree->root;
  p = tree->root.link[0];
  for (;;)
    {
      int diff = tree->cmp (item, p->data, tree->param);

      if (diff == 0)
	break;

      pa[k] = p;
      if (diff < 0)
	p = p->link[0], a[k] = 0;
      else if (diff > 0)
	p = p->link[1], a[k] = 1;
      k++;

      if (p == NULL)
	  {
		  internal_unlock_mutex(&tree->mutex);
	return NULL;
	  }
    }
  tree->count--;
  
  item = p->data;

  q = &pa[k - 1]->link[(int) a[k - 1]];
  if (p->link[1] == NULL)
    {
      *q = p->link[0];
      if (*q)
	(*q)->bal = 0;
    }
  else
    {
      avl_node *r = p->link[1];
      if (r->link[0] == NULL)
	{
	  r->link[0] = p->link[0];
	  *q = r;
	  r->bal = p->bal;
	  a[k] = 1;
	  pa[k++] = r;
	}
      else
	{
	  avl_node *s = r->link[0];
	  int l = k++;

	  a[k] = 0;
	  pa[k++] = r;
	    
	  while (s->link[0] != NULL)
	    {
	      r = s;
	      s = r->link[0];
	      a[k] = 0;
	      pa[k++] = r;
	    }

	  a[l] = 1;
	  pa[l] = s;
	  s->link[0] = p->link[0];
	  r->link[0] = s->link[1];
	  s->link[1] = p->link[1];
	  s->bal = p->bal;
	  *q = s;
	}
    }

#if PSPP
  if (tree->owner == NULL)
#endif
    free (p);

  assert (k > 0);
  while (--k)
    {
      avl_node *s = pa[k], *r;

      if (a[k] == 0)
	{
	  if (s->bal == -1)
	    {
	      s->bal = 0;
	      continue;
	    }
	  else if (s->bal == 0)
	    {
	      s->bal = 1;
	      break;
	    }

	  assert (s->bal == +1);
	  r = s->link[1];

	  assert (r != NULL);
	  if (r->bal == 0)
	    {
	      s->link[1] = r->link[0];
	      r->link[0] = s;
	      r->bal = -1;
	      pa[k - 1]->link[(int) a[k - 1]] = r;
	      break;
	    }
	  else if (r->bal == +1)
	    {
	      s->link[1] = r->link[0];
	      r->link[0] = s;
	      s->bal = r->bal = 0;
	      pa[k - 1]->link[(int) a[k - 1]] = r;
	    }
	  else 
	    {
	      assert (r->bal == -1);
	      p = r->link[0];
	      r->link[0] = p->link[1];
	      p->link[1] = r;
	      s->link[1] = p->link[0];
	      p->link[0] = s;
	      if (p->bal == +1)
		s->bal = -1, r->bal = 0;
	      else if (p->bal == 0)
		s->bal = r->bal = 0;
	      else
		{
		  assert (p->bal == -1);
		  s->bal = 0, r->bal = +1;
		}
	      p->bal = 0;
	      pa[k - 1]->link[(int) a[k - 1]] = p;
	    }
	}
      else
	{
	  assert (a[k] == 1);

	  if (s->bal == +1)
	    {
	      s->bal = 0;
	      continue;
	    }
	  else if (s->bal == 0)
	    {
	      s->bal = -1;
	      break;
	    }

	  assert (s->bal == -1);
	  r = s->link[0];

	  if (r == NULL || r->bal == 0)
	    {
	      s->link[0] = r->link[1];
	      r->link[1] = s;
	      r->bal = 1;
	      pa[k - 1]->link[(int) a[k - 1]] = r;
	      break;
	    }
	  else if (r->bal == -1)
	    {
	      s->link[0] = r->link[1];
	      r->link[1] = s;
	      s->bal = r->bal = 0;
	      pa[k - 1]->link[(int) a[k - 1]] = r;
	    }
	  else if (r->bal == +1)
	    {
	      p = r->link[1];
	      r->link[1] = p->link[0];
	      p->link[0] = r;
	      s->link[0] = p->link[1];
	      p->link[1] = s;
	      if (p->bal == -1)
		s->bal = 1, r->bal = 0;
	      else if (p->bal == 0)
		s->bal = r->bal = 0;
	      else
		{
		  assert (p->bal == 1);
		  s->bal = 0, r->bal = -1;
		}
	      p->bal = 0;
	      pa[k - 1]->link[(int) a[k - 1]] = p;
	    }
	}
    }
      
	internal_unlock_mutex(&tree->mutex);
  return (void *) item;
}

void *
avl_insert (avl_tree *tree, void *item)
{
  void **p;
  
  assert (tree != NULL);
  
  p = avl_probe (tree, item);
  return (*p == item) ? NULL : *p;
}

void *
avl_replace (avl_tree *tree, void *item)
{
  void **p;

  assert (tree != NULL);
  
  p = avl_probe (tree, item);
  if (*p == item)
    return NULL;
  else
    {
      void *r = *p;
      *p = item;
      return r;
    }
}

void *
(avl_force_delete) (avl_tree *tree, void *item)
{
  void *found = avl_delete (tree, item);
  assert (found != NULL);
  return found;
}

#if SELF_TEST

int done = 0;

void
print_structure (avl_node *node, int level)
{
  char lc[] = "([{`/";
  char rc[] = ")]}'\\";

  assert (level <= 10);
  
  if (node == NULL)
    {
      printf (" nil");
      return;
    }
  printf (" %c%d", lc[level % 5], (int) node->data);
  if (node->link[0] || node->link[1])
    print_structure (node->link[0], level + 1);
  if (node->link[1])
    print_structure (node->link[1], level + 1);
  printf ("%c", rc[level % 5]);
}

int
compare_ints (const void *a, const void *b, void *param unused)
{
  return ((int) a) - ((int) b);
}

void
print_int (void *a, void *param unused)
{
  printf (" %d", (int) a);
}

int
recurse_tree (avl_node *node, int *count, int parent, int dir)
{
  if (node) 
    {
      int d = (int) node->data;
      int nl = node->link[0] ? recurse_tree (node->link[0], count, d, -1) : 0;
      int nr = node->link[1] ? recurse_tree (node->link[1], count, d, 1) : 0;
      (*count)++;

      if (nr - nl != node->bal)
	printf (" Node %d is unbalanced: right height=%d, left height=%d, "
		"difference=%d, but balance factor=%d.\n", d, nr, nl, nr - nl, node->bal), done = 1;
      if (parent != INT_MIN)
	{
	  assert (dir == -1 || dir == +1);
	  if (dir == -1 && d > parent)
	    printf (" Node %d is smaller than its left child %d.\n",
		    parent, d), done = 1;
	  else if (dir == +1 && d < parent)
	    printf (" Node %d is larger than its right child %d.\n",
		    parent, d), done = 1;
	}
      assert (node->bal >= -1 && node->bal <= 1);
      return 1 + (nl > nr ? nl : nr);
    }
  else return 0;
}

void
verify_tree (avl_tree *tree)
{
  int count = 0;
  recurse_tree (tree->root.link[0], &count, INT_MIN, 0);
  if (count != tree->count)
    printf (" Tree has %d nodes, but tree count is %d.\n", count, tree->count), done = 1;
  if (done)
    abort ();
}

void
shuffle (int *array, int n)
{
  int i;
  
  for (i = 0; i < n; i++)
    {
      int j = i + rand () % (n - i);
      int t = array[j];
      array[j] = array[i];
      array[i] = t;
    }
}

void
compare_trees (avl_node *a, avl_node *b)
{
  if (a == NULL || b == NULL)
    {
      assert (a == NULL && b == NULL);
      return;
    }
  if (a->data != b->data || a->bal != b->bal
      || ((a->link[0] != NULL) ^ (b->link[0] != NULL))
      || ((a->link[1] != NULL) ^ (b->link[1] != NULL)))
    {
      printf (" Copied nodes differ: %d b=%d a->bal=%d b->bal=%d a:",
	      (int) a->data, (int) b->data, a->bal, b->bal);
      if (a->link[0])
	printf ("l");
      if (a->link[1])
	printf ("r");
      printf (" b:");
      if (b->link[0])
	printf ("l");
      if (b->link[1])
	printf ("r");
      printf ("\n");
      abort ();
    }
  if (a->link[0] != NULL)
    compare_trees (a->link[0], b->link[0]);
  if (a->link[1] != NULL)
    compare_trees (a->link[1], b->link[1]);
}

/* Simple stress test procedure for the AVL tree routines.  Does the
   following:

   * Generate a random number seed.  By default this is generated from
   the current time.  You can also pass a seed value on the command
   line if you want to test the same case.  The seed value is
   displayed.

   * Create a tree and insert the integers from 0 up to TREE_SIZE - 1
   into it, in random order.  Verify the tree structure after each
   insertion.
   
   * Remove each integer from the tree, in a different random order.
   After each deletion, verify the tree structure; also, make a copy
   of the tree into a new tree, verify the copy and compare it to the
   original, then destroy the copy.

   * Destroy the tree, increment the random seed value, and start over.

   If you make any modifications to the avl tree routines, then you
   might want to insert some calls to print_structure() at strategic
   places in order to be able to see what's really going on.  Also,
   memory debuggers like Checker or Purify are very handy. */
#define TREE_SIZE 1024
#define N_ITERATIONS 2
int
main (int argc, char **argv)
{
  int array[TREE_SIZE];
  int seed;
  int iteration;
  
  if (argc == 2)
    seed = atoi (argv[1]);
  else
    seed = time (0) * 257 % 32768;

  fputs ("Testing avl...\n", stdout);
  
  for (iteration = 1; iteration <= N_ITERATIONS; iteration++)
    {
      avl_tree *tree;
      int i;
      
      printf ("Iteration %4d/%4d: seed=%5d", iteration, N_ITERATIONS, seed);
      fflush (stdout);
      
      srand (seed++);

      for (i = 0; i < TREE_SIZE; i++)
	array[i] = i;
      shuffle (array, TREE_SIZE);
      
      tree = avl_create (compare_ints, NULL);
      for (i = 0; i < TREE_SIZE; i++)
	avl_force_insert (tree, (void *) (array[i]));
      verify_tree (tree);

      shuffle (array, TREE_SIZE);
      for (i = 0; i < TREE_SIZE; i++)
	{
	  avl_tree *copy;

	  avl_delete (tree, (void *) (array[i]));
	  verify_tree (tree);

	  copy = avl_copy (tree, NULL);
	  verify_tree (copy);
	  compare_trees (tree->root.link[0], copy->root.link[0]);
	  avl_destroy (copy, NULL);

	  if (i % 128 == 0)
	    {
	      putchar ('.');
	      fflush (stdout);
	    }
	}
      fputs (" good.\n", stdout);

      avl_destroy (tree, NULL);
    }
  
  return 0;
}
#endif /* SELF_TEST */

/* avl_functions.c. ajd *****************************************************************/

int
compare_users (const void *first, const void *second, void *param)
{
	ice_user_t *v1 = (ice_user_t *) first, *v2 = (ice_user_t *) second;
	
	if (!first || !second || !v1->name || !v2->name)
	{
		xa_debug (2, "WARNING: compare_users called with NULL pointers!");
		return 0;
	}

	return (ice_strcmp (v1->name, v2->name));
}


int
compare_mounts (const void *first, const void *second, void *param)
{
	mount_t *v1 = (mount_t *) first, *v2 = (mount_t *) second;
	
	if (!first || !second || !v1->name || !v2->name)
	{
		xa_debug (2, "WARNING: compare_mounts called with NULL pointers!");
		return 0;
	}

	return (ice_strcmp (v1->name, v2->name));
}

int
compare_vars (const void *first, const void *second, void *param)
{
	varpair_t *v1 = (varpair_t *) first, *v2 = (varpair_t *) second;
	
	if (!first || !second || !v1->name || !v2->name)
	{
		xa_debug (2, "WARNING: compare_vars called with NULL pointers!");
		return 0;
	}

	return (ice_strcmp (v1->name, v2->name));
}
	
#ifdef DEBUG_MEMORY
int
compare_mem (const void *firstp, const void *secondp, void *param)
{
	meminfo_t *first = (meminfo_t *) firstp, *second = (meminfo_t *) secondp;

	if (!first || !second)
	{
		fprintf (stderr, "WARNING, compare_mem called with NULL pointers!");
		return 0;
	}

        if ((unsigned long int) (first->ptr) > (unsigned long int) (second->ptr))
		return 1;
	else if ((unsigned long int) (first->ptr) < (unsigned long int) (second->ptr))
		return -1;
	return 0;
}
#endif

int
compare_strings (const void *first, const void *second, void *param)
{
	char *a1 = (char *)first, *a2 = (char *)second;

	if (!first || !second)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: compare_strings called with null pointers");
		return 0;
	}

	xa_debug (4, "DEBUG: Comparing [%s] with [%s]", a1, a2);
	return (ice_strcasecmp (a1, a2));
}

int
compare_threads (const void *first, const void *second, void *param)
{
	mythread_t *t1, *t2;
	t1 = (mythread_t *)first;
	t2 = (mythread_t *)second;

	if (!first || !second)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: compare_threads called with NULL pointers");
		return 0;
	}

	if (t1->id > t2->id)
		return 1;
	if (t1->id < t2->id)
		return -1;
	return 0;
}

int
compare_mutexes (const void *first, const void *second, void *param)
{
	mutex_t *t1, *t2;
	t1 = (mutex_t *)first;
	t2 = (mutex_t *)second;

	if (!first || !second)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: compare_mutex called with NULL pointers");
		return 0;
	}

	if (t1->mutexid > t2->mutexid)
		return 1;
	if (t1->mutexid < t2->mutexid)
		return -1;
	return 0;
}

int compare_connection(const void *first, const void *second, void *param)
{
	connection_t *a1, *a2;

	if (!first || !second)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING!!! - Null pointer connection!");
		return -1;
	}

	a1 = (connection_t *)first;
	a2 = (connection_t *)second;

	if (a1->type != a2->type)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING!!!! - Comparing different type connections");
		return -1;
	}

	if (a1->id > a2->id)
		return 1;
	else if (a1->id < a2->id)
		return -1;
	else
		return 0;
}

void
zero_trav(avl_traverser *trav)
{
	if (!trav)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: zero_trav called with NULL trav");
		return;
	}
	trav->init = 0;
	trav->nstack = 0;
	trav->p = NULL;
}


int
compare_sockets (const void *first, const void *second, void *param)
{
	ice_socket_t *is1 = (ice_socket_t *) first, *is2 = (ice_socket_t *) second;
	
	if (is1 == NULL || is2 == NULL) {
		fprintf (stderr, "WARNING: compare_sockets called with NULL values");
		return -1;
	} else if (is1->sock < 0 || is2->sock < 0) {
		fprintf (stderr, "WARNING: compare_sockets called with negative socket number");
		return -1;
	}

	if (is1->sock > is2->sock)
		return 1;
	else if (is1->sock < is2->sock)
		return -1;
	return 0;
}

void *
avl_get_any_node (avl_tree *tree)
{
	avl_traverser trav = {0};
	if (!tree)
	{
		android_log (ANDROID_LOG_VERBOSE, "WARNING: avl_get_any_node called with NULL tree");
		return NULL;
	}

	if (avl_count (tree) <= 0)
		return NULL;

	return (avl_traverse (tree, &trav));
}


